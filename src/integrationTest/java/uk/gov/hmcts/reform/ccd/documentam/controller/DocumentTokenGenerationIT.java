package uk.gov.hmcts.reform.ccd.documentam.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.AuditOperationType;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_JURISDICTION_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.DOCUMENTS_URL;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubGetDocumentMetaData;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubNotFoundGetDocumentMetaData;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubPatchDocumentMetaDataFailure;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubPatchDocumentMetaData;

// Fix the test salt so the expected SHA-256 values are independent of the production hashing helper.
@TestPropertySource(properties = "idam.s2s-auth.totp_secret=AABBCCDDEEFFGGHH")
class DocumentTokenGenerationIT extends BaseTest implements TestFixture {

    private static final String SERVICE_NAME_BULK_SCAN_ORCHESTRATOR = "bulk_scan_orchestrator";
    private static final String SERVICE_NAME_BULK_SCAN_PROCESSOR = "bulk_scan_processor";
    private static final String TOKEN_URL = "/cases/documents/" + DOCUMENT_ID + "/token";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetRequestJournal() {
        resetAllRequests();
    }

    @Test
    void shouldGenerateTokenForDocumentWithCompleteMetadata() throws Exception {
        final Document document = Document.builder()
            .metadata(Map.of(
                METADATA_CASE_ID, CASE_ID_VALUE,
                METADATA_CASE_TYPE_ID, CASE_TYPE_ID_VALUE,
                METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE))
            .build();
        stubGetDocumentMetaData(document);

        mockMvc.perform(get(TOKEN_URL)
                            .headers(createHttpHeaders(SERVICE_NAME_BULK_SCAN_ORCHESTRATOR)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.hashToken",
                                is("e540bdf626570d8b31cb9cb6b6ae7eb37c9d851aeef1e6dee43f32dbc0854515")))
            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.GENERATE_HASH_CODE,
                SERVICE_NAME_BULK_SCAN_ORCHESTRATOR,
                List.of(DOCUMENT_ID.toString()),
                null,
                null,
                null));

        verify(getRequestedFor(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID)));
        verify(0, patchRequestedFor(urlPathEqualTo("/documents")));
    }

    @Test
    void shouldGenerateTokenAndPatchPermittedDefaultCaseType() throws Exception {
        final Document document = Document.builder()
            .metadata(Map.of(METADATA_JURISDICTION_ID, "BULKSCAN"))
            .build();
        stubGetDocumentMetaData(document);
        stubPatchDocumentMetaData(document);

        mockMvc.perform(get(TOKEN_URL)
                            .headers(createHttpHeaders(SERVICE_NAME_BULK_SCAN_ORCHESTRATOR)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.hashToken",
                                is("47090957d02cbc9117ad35eb426326a7d794cfa1ae6865e3ea12cd49508bd2ef")))
            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.GENERATE_HASH_CODE,
                SERVICE_NAME_BULK_SCAN_ORCHESTRATOR,
                List.of(DOCUMENT_ID.toString()),
                null,
                null,
                null));

        verify(1, patchRequestedFor(urlPathEqualTo("/documents")));
        verify(patchRequestedFor(urlPathEqualTo("/documents"))
                   .withRequestBody(equalToJson("""
                       {
                         "ttl": null,
                         "documents": [{
                           "documentId": "%s",
                           "metadata": {
                             "case_type_id": "BULKSCAN_Exception",
                             "jurisdiction": "BULKSCAN"
                           }
                         }]
                       }
                       """.formatted(DOCUMENT_ID))));
    }

    @Test
    void shouldGenerateTokenAndPatchPermittedDefaultJurisdiction() throws Exception {
        final Document document = Document.builder()
            .metadata(Map.of(METADATA_CASE_TYPE_ID, CASE_TYPE_ID_VALUE))
            .build();
        stubGetDocumentMetaData(document);
        stubPatchDocumentMetaData(document);

        mockMvc.perform(get(TOKEN_URL)
                            .headers(createHttpHeaders(SERVICE_NAME_BULK_SCAN_ORCHESTRATOR)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.hashToken").exists());

        verify(1, patchRequestedFor(urlPathEqualTo("/documents")));
        verify(patchRequestedFor(urlPathEqualTo("/documents"))
                   .withRequestBody(equalToJson("""
                       {
                         "ttl": null,
                         "documents": [{
                           "documentId": "%s",
                           "metadata": {
                             "case_type_id": "%s",
                             "jurisdiction": "AUTOTEST1"
                           }
                         }]
                       }
                       """.formatted(DOCUMENT_ID, CASE_TYPE_ID_VALUE))));
    }

    @Test
    void shouldRejectTokenGenerationWhenServiceLacksPermission() throws Exception {
        final Document document = Document.builder()
            .metadata(Map.of(
                METADATA_CASE_TYPE_ID, CASE_TYPE_ID_VALUE,
                METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE))
            .build();
        stubGetDocumentMetaData(document);

        mockMvc.perform(get(TOKEN_URL)
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP)))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", is("Forbidden: Insufficient permissions: " + DOCUMENT_ID)))
            .andExpect(jsonPath("$.hashToken").doesNotExist())
            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.GENERATE_HASH_CODE,
                SERVICE_NAME_XUI_WEBAPP,
                List.of(DOCUMENT_ID.toString()),
                null,
                null,
                null));

        verify(getRequestedFor(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID)));
        verify(0, patchRequestedFor(urlPathEqualTo("/documents")));
    }

    @Test
    void shouldRejectTokenGenerationForBulkScanProcessor() throws Exception {
        final Document document = Document.builder()
            .metadata(Map.of(
                METADATA_CASE_TYPE_ID, CASE_TYPE_ID_VALUE,
                METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE))
            .build();
        stubGetDocumentMetaData(document);

        mockMvc.perform(get(TOKEN_URL)
                            .headers(createHttpHeaders(SERVICE_NAME_BULK_SCAN_PROCESSOR)))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", is("Forbidden: Insufficient permissions: " + DOCUMENT_ID)))
            .andExpect(jsonPath("$.hashToken").doesNotExist())
            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.GENERATE_HASH_CODE,
                SERVICE_NAME_BULK_SCAN_PROCESSOR,
                List.of(DOCUMENT_ID.toString()),
                null,
                null,
                null));

        verify(getRequestedFor(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID)));
        verify(0, patchRequestedFor(urlPathEqualTo("/documents")));
    }

    @Test
    void shouldReturnNotFoundWhenDocumentDoesNotExist() throws Exception {
        stubNotFoundGetDocumentMetaData(DOCUMENT_ID);

        mockMvc.perform(get(TOKEN_URL)
                            .headers(createHttpHeaders(SERVICE_NAME_BULK_SCAN_ORCHESTRATOR)))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", is("Meta data does not exist for documentId: " + DOCUMENT_ID)))
            .andExpect(jsonPath("$.hashToken").doesNotExist());

        verify(getRequestedFor(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID)));
        verify(0, patchRequestedFor(urlPathEqualTo("/documents")));
    }

    @Test
    void shouldRejectTokenGenerationWithoutUserBearerToken() throws Exception {
        final HttpHeaders headers = createHttpHeaders(SERVICE_NAME_BULK_SCAN_ORCHESTRATOR);
        headers.remove(HttpHeaders.AUTHORIZATION);

        assertUnauthorizedWithoutDocumentLookup(headers);
    }

    @Test
    void shouldRejectTokenGenerationWithExpiredUserBearerToken() throws Exception {
        final HttpHeaders headers = createHttpHeaders(-1, SERVICE_NAME_BULK_SCAN_ORCHESTRATOR, AUTH_TOKEN_TTL);

        assertUnauthorizedWithoutDocumentLookup(headers);
    }

    @Test
    void shouldRejectTokenGenerationWithoutServiceAuthorizationToken() throws Exception {
        final HttpHeaders headers = createHttpHeaders(SERVICE_NAME_BULK_SCAN_ORCHESTRATOR);
        headers.remove(SERVICE_AUTHORIZATION);

        assertUnauthorizedWithoutDocumentLookup(headers);
    }

    @Test
    void shouldRejectTokenGenerationWithExpiredServiceAuthorizationToken() throws Exception {
        final HttpHeaders headers = createHttpHeaders(
            AUTH_TOKEN_TTL,
            SERVICE_NAME_BULK_SCAN_ORCHESTRATOR,
            -1
        );

        assertUnauthorizedWithoutDocumentLookup(headers);
    }

    @Test
    void shouldNotReturnTokenWhenMetadataDefaultingPatchFails() throws Exception {
        final Document document = Document.builder()
            .metadata(Map.of(METADATA_JURISDICTION_ID, "BULKSCAN"))
            .build();
        stubGetDocumentMetaData(document);
        stubPatchDocumentMetaDataFailure();

        mockMvc.perform(get(TOKEN_URL)
                            .headers(createHttpHeaders(SERVICE_NAME_BULK_SCAN_ORCHESTRATOR)))
            .andExpect(status().isBadGateway())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.hashToken").doesNotExist());

        verify(getRequestedFor(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID)));
        verify(1, patchRequestedFor(urlPathEqualTo("/documents")));
    }

    private void assertUnauthorizedWithoutDocumentLookup(final HttpHeaders headers) throws Exception {
        mockMvc.perform(get(TOKEN_URL).headers(headers))
            .andExpect(status().isUnauthorized());

        verify(0, getRequestedFor(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID)));
        verify(0, patchRequestedFor(urlPathEqualTo("/documents")));
    }
}
