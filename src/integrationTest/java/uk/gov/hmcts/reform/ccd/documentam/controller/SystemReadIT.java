package uk.gov.hmcts.reform.ccd.documentam.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_JURISDICTION_ID;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentBinaryContent;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentUrlNoPermissions;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubGetDocumentMetaData;

class SystemReadIT extends BaseTest implements TestFixture {

    private static final String DOCUMENT_URL = "/cases/documents/" + DOCUMENT_ID;
    private static final String BINARY_URL = DOCUMENT_URL + "/binary";
    private static final String CCD_GW = "ccd_gw";
    private static final String CCD_DOCUMENT_URL = "/cases/" + CASE_ID_VALUE + "/documents/" + DOCUMENT_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationParams applicationParams;

    @Autowired
    private AuthorisedServices authorisedServices;

    @BeforeEach
    void setUp() {
        resetAllRequests();
        // The IDAM user_info stub gives the user the roles caseworker-test and pui-caa
        allowSystemRead(CCD_GW, "caseworker-test");
        allowSystemRead("civil_service", "caseworker-missing");
        // CCD would deny the user, so any request reaching CCD is forbidden
        stubDocumentUrlNoPermissions();
        stubDocumentBinaryContent();
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(applicationParams, "isStreamDownloadEnabled", false);
        allowSystemRead(CCD_GW, null);
        allowSystemRead("civil_service", null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/binary"})
    void shouldReadWithoutCallingCcdWhenAllowed(String endpoint) throws Exception {
        stubGetDocumentMetaData(document(JURISDICTION_ID_VALUE, CASE_TYPE_ID_VALUE));

        mockMvc.perform(get(DOCUMENT_URL + endpoint).headers(createHttpHeaders(CCD_GW)))
            .andExpect(status().isOk());

        verify(0, getRequestedFor(urlPathEqualTo(CCD_DOCUMENT_URL)));
    }

    @Test
    void shouldStreamWithoutCallingCcdWhenAllowed() throws Exception {
        ReflectionTestUtils.setField(applicationParams, "isStreamDownloadEnabled", true);
        stubGetDocumentMetaData(document(JURISDICTION_ID_VALUE, CASE_TYPE_ID_VALUE));

        mockMvc.perform(get(BINARY_URL).headers(createHttpHeaders(CCD_GW)))
            .andExpect(status().isOk());

        verify(0, getRequestedFor(urlPathEqualTo(CCD_DOCUMENT_URL)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/binary"})
    void shouldUseCcdWhenServiceNotConfigured(String endpoint) throws Exception {
        stubGetDocumentMetaData(document(JURISDICTION_ID_VALUE, CASE_TYPE_ID_VALUE));

        assertCcdDecides(DOCUMENT_URL + endpoint, SERVICE_NAME_XUI_WEBAPP);
    }

    @Test
    void shouldUseCcdWhenUserLacksTheRole() throws Exception {
        stubGetDocumentMetaData(document("CIVIL", "CIVIL"));

        assertCcdDecides(DOCUMENT_URL, "civil_service");
    }

    @Test
    void shouldNotReadOutsideTheServicesOwnCaseTypes() throws Exception {
        stubGetDocumentMetaData(document(JURISDICTION_ID_VALUE, "OTHER_CASE_TYPE"));

        mockMvc.perform(get(BINARY_URL).headers(createHttpHeaders(CCD_GW)))
            .andExpect(status().isForbidden());

        verify(0, getRequestedFor(urlPathEqualTo("/documents/" + DOCUMENT_ID + "/binary")));
    }

    private void assertCcdDecides(String url, String serviceName) throws Exception {
        mockMvc.perform(get(url).headers(createHttpHeaders(serviceName)))
            .andExpect(status().isForbidden());

        verify(1, getRequestedFor(urlPathEqualTo(CCD_DOCUMENT_URL)));
        verify(0, getRequestedFor(urlPathEqualTo("/documents/" + DOCUMENT_ID + "/binary")));
    }

    private void allowSystemRead(String serviceId, String role) {
        AuthorisedService service = authorisedServices.getAuthServices().stream()
            .filter(authorisedService -> authorisedService.getId().equals(serviceId))
            .findFirst()
            .orElseThrow();
        service.setSystemReadRole(role);
    }

    private static Document document(String jurisdictionId, String caseTypeId) {
        return Document.builder()
            .classification(Classification.PUBLIC)
            .metadata(Map.of(
                METADATA_CASE_ID, CASE_ID_VALUE,
                METADATA_JURISDICTION_ID, jurisdictionId,
                METADATA_CASE_TYPE_ID, caseTypeId))
            .links(TestFixture.getLinks())
            .build();
    }
}
