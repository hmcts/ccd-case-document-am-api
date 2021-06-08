package uk.gov.hmcts.reform.ccd.documentam.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.controller.endpoints.CaseDocumentAmController;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.util.ApplicationUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)

public class CaseDocumentAmControllerIT extends BaseTest {

    @Value("${idam.s2s-auth.totp_secret}")
    protected String salt;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private static final String MAIN_URL = "/cases/documents";
    private static final String ATTACH_TO_CASE_URL = "/attachToCase";
    private static final String DOCUMENTS_URL = "/documents/";
    private static final String SERVICE_NAME = "xui_webapp";

    private static final String CASE_ID_KEY = "caseId";
    private static final String CLASSIFICATION_KEY = "classification";
    private static final String CASE_TYPE_ID_KEY = "caseTypeId";
    private static final String JURISDICTION_ID_KEY = "jurisdictionId";

    private static final String CLASSIFICATION = "PUBLIC";
    private static final String CASE_TYPE_ID = "BEFTA_CASETYPE_2";
    private static final String JURISDICTION_ID = "BEFTA_JURISDICTION_2";

    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final String CASE_ID = "1584722156538291";
    private static final String USER_ID = "d5566a63-f87c-4658-a4d6-213d949f8415";

    private static final String SERVICE_AUTHORISATION_KEY = "ServiceAuthorization";
    private static final String BEARER = "Bearer ";
    private static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9.WWRzROlKxLQCJw5h0h0dHb9hHfbBhF2Idwv1z4L4FnqSw3VZ38ZRLuDmwr3tj-8oOv6EfLAxV0dJAPtUT203Iw";
    private static final String SERVICE_AUTHORISATION_VALUE = BEARER + TOKEN;


    @Test
    void shouldSuccessfullyUploadDocument() throws Exception {
        MockMultipartFile firstFile =
            new MockMultipartFile("files", "filename.txt",
                                  "text/plain", "some xml".getBytes());
        MockMultipartFile secondFile =
            new MockMultipartFile("data", "other-file-name.data",
                                  "text/plain", "some other type".getBytes());
        MockMultipartFile jsonFile =
            new MockMultipartFile("json", "",
                                  MediaType.APPLICATION_JSON_VALUE, "{\"json\": \"someValue\"}".getBytes());

        stubDocumentUrl();
        stubUploadDocument();
        stubDocumentManagementUploadDocument();

        MockMvc mockMvcBuilder
            = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockMvcBuilder.perform(MockMvcRequestBuilders.multipart(MAIN_URL)
                            .file(firstFile)
                            .file(secondFile)
                            .file(jsonFile)
                            .headers(createHttpHeaders(SERVICE_NAME))
                            .param(CLASSIFICATION_KEY, CLASSIFICATION)
                            .param(CASE_TYPE_ID_KEY, CASE_TYPE_ID)
                            .param(JURISDICTION_ID_KEY, JURISDICTION_ID)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
            .andExpect(status().isOk())
            .andReturn();
    }


    @Test
    void shouldSuccessfullyGetDocumentByDocumentId() throws Exception {
        String metaDataJsonExpression = "$.metadata.";

        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResource();

        stubDocumentUrl();
        stubGetDocumentMetaData(storedDocumentResource);

        mockMvc.perform(get(MAIN_URL + "/" +  DOCUMENT_ID)
                            .headers(createHttpHeaders(SERVICE_NAME)))
            .andExpect(status().isOk())
            .andExpect(jsonPath(metaDataJsonExpression + CASE_ID_KEY, is(CASE_ID)))
            .andExpect(jsonPath(metaDataJsonExpression + CASE_TYPE_ID_KEY, is(CASE_TYPE_ID)))
            .andExpect(jsonPath(metaDataJsonExpression + JURISDICTION_ID_KEY, is(JURISDICTION_ID)))
            .andExpect(jsonPath("$._links.self.href",
                                is("http://localhost" + MAIN_URL + "/" + DOCUMENT_ID)))
            .andReturn();
    }

    @Test
    void shouldSuccessfullyDeleteDocumentByDocumentId() throws Exception {
        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResource();

        stubDocumentUrl();
        stubGetDocumentMetaData(storedDocumentResource);
        stubDeleteDocumentByDocumentId();

        mockMvc.perform(delete(MAIN_URL + "/" + DOCUMENT_ID)
                            .headers(createHttpHeaders(SERVICE_NAME)))
            .andExpect(status().isNoContent())
            .andReturn();
    }

    @Test
    void shouldSuccessfullyGetDocumentBinaryContent() throws Exception {
        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResource();

        stubDocumentUrl();
        stubGetDocumentMetaData(storedDocumentResource);
        stubDocumentBinaryContent();

        mockMvc.perform(get(MAIN_URL + "/" + DOCUMENT_ID + "/binary")
                            .headers(createHttpHeaders(SERVICE_NAME)))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void shouldSuccessfullyPatchDocumentByDocumentId() throws Exception {
        UpdateDocumentCommand body = new UpdateDocumentCommand();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        String formattedTTL = dateFormat.format(new Timestamp(new Date().getTime() + Long.parseLong("600000")));
        body.setTtl(formattedTTL);

        Date time = Date.from(Instant.now());

        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResourceToUpdatePatch(time);

        stubDocumentUrl();
        stubGetDocumentMetaData(storedDocumentResource);
        stubPatchDocument(storedDocumentResource);

        mockMvc.perform(patch(MAIN_URL + "/" + DOCUMENT_ID)
                            .headers(createHttpHeaders(SERVICE_NAME))
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(getJsonString(body)))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void shouldSuccessfullyPatchMetaDataOnDocument() throws Exception {
        String hashToken = ApplicationUtils
            .generateHashCode(salt.concat(DOCUMENT_ID.toString()
                                              .concat(CASE_ID)
                                              .concat(JURISDICTION_ID)
                                              .concat(CASE_TYPE_ID)));

        List<DocumentHashToken> documentHashTokens = new ArrayList<>();
        documentHashTokens.add(new DocumentHashToken(DOCUMENT_ID.toString(), hashToken));
        CaseDocumentsMetadata body = new CaseDocumentsMetadata();
        body.setDocumentHashTokens(documentHashTokens);
        body.setCaseId(CASE_ID);
        body.setCaseTypeId(CASE_TYPE_ID);
        body.setJurisdictionId(JURISDICTION_ID);

        Date time = Date.from(Instant.now());

        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResourceToUpdatePatch(time);

        stubDocumentUrl();
        stubGetDocumentMetaData(storedDocumentResource);
        stubPatchDocumentMetaData(storedDocumentResource);

        mockMvc.perform(patch(MAIN_URL + ATTACH_TO_CASE_URL)
                            .headers(createHttpHeaders("ccd_data"))
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(getJsonString(body)))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void shouldBeForbiddenWhenPatchingMetaDataOnDocumentWithIncorrectDocumentHashToken() throws Exception {
        List<DocumentHashToken> documentHashTokens = new ArrayList<>();
        documentHashTokens.add(new DocumentHashToken(DOCUMENT_ID.toString(), "567890976546789"));
        CaseDocumentsMetadata body = new CaseDocumentsMetadata();
        body.setDocumentHashTokens(documentHashTokens);
        body.setCaseId(CASE_ID);
        body.setCaseTypeId(CASE_TYPE_ID);
        body.setJurisdictionId(JURISDICTION_ID);

        Date time = Date.from(Instant.now());

        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResourceToUpdatePatch(time);

        stubDocumentUrl();
        stubGetDocumentMetaData(storedDocumentResource);
        stubPatchDocumentMetaData(storedDocumentResource);

        mockMvc.perform(patch(MAIN_URL + ATTACH_TO_CASE_URL)
                            .headers(createHttpHeaders("ccd_data"))
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(getJsonString(body)))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    private static void stubDocumentUrl(){
        CaseDocumentMetadata caseDocumentMetadata = new CaseDocumentMetadata();
        caseDocumentMetadata.setCaseId(CASE_ID);

        List<Permission> permissionList = new ArrayList<>();
        permissionList.add(Permission.READ);
        DocumentPermissions documentPermissions = new DocumentPermissions();
        documentPermissions.setId(DOCUMENT_ID.toString());
        documentPermissions.setPermissions(permissionList);

        caseDocumentMetadata.setDocumentPermissions(documentPermissions);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("documentMetadata", caseDocumentMetadata);

        stubFor(WireMock.get(WireMock.urlPathEqualTo("/cases/" + CASE_ID + DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORISATION_KEY, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .withStatus(HttpStatus.OK.value())
                                    .withBody(getJsonString(body))
                    )
        );
    }

    private static void stubUploadDocument(){
        stubFor(WireMock.post(WireMock.urlPathEqualTo(MAIN_URL))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    private static void stubDocumentManagementUploadDocument(){
        stubFor(WireMock.post(WireMock.urlPathEqualTo("/documents"))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    private static void stubGetDocumentMetaData(StoredDocumentHalResource storedDocumentHalResource) {
        stubFor(WireMock.get(WireMock.urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(storedDocumentHalResource))
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    private static void stubDeleteDocumentByDocumentId() {
        stubFor(WireMock.delete(WireMock.urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .withQueryParam("permanent", equalTo("false"))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_NO_CONTENT)));
    }

    private static void stubDocumentBinaryContent() {
        stubFor(WireMock.get(WireMock.urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID + "/binary"))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withHeader("OriginalFileName", "")
                                    .withHeader("Content-Disposition", "")
                                    .withHeader("Data-Source", "")
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    private static void stubPatchDocument(StoredDocumentHalResource storedDocumentHalResource) {
        stubFor(WireMock.patch(WireMock.urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(storedDocumentHalResource))
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    private static void stubPatchDocumentMetaData(StoredDocumentHalResource storedDocumentHalResource) {
        stubFor(WireMock.patch(WireMock.urlPathEqualTo("/documents"))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(storedDocumentHalResource))
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    private StoredDocumentHalResource getStoredDocumentResource(){

        HashMap<String, String> metaData = new HashMap<>();
        metaData.put(CASE_ID_KEY, CASE_ID);
        metaData.put(CASE_TYPE_ID_KEY, CASE_TYPE_ID);
        metaData.put(JURISDICTION_ID_KEY, JURISDICTION_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setClassification(StoredDocumentHalResource.ClassificationEnum.PUBLIC);
        storedDocumentHalResource.setCreatedBy(USER_ID);
        storedDocumentHalResource.setCreatedOn(Date.from(Instant.now()));
        storedDocumentHalResource.setLastModifiedBy(USER_ID);
        storedDocumentHalResource.setMetadata(metaData);

        return storedDocumentHalResource;
    }

    private StoredDocumentHalResource getStoredDocumentResourceToUpdatePatch(Date time){
        HashMap<String, String> metaData = new HashMap<>();
        metaData.put("size", "10");
        metaData.put("createdBy", USER_ID);
        metaData.put(CASE_ID_KEY, CASE_ID);
        metaData.put(CASE_TYPE_ID_KEY, CASE_TYPE_ID);
        metaData.put(JURISDICTION_ID_KEY, JURISDICTION_ID);
        metaData.put(CLASSIFICATION_KEY, CLASSIFICATION);
        metaData.put("metadata", "");
        metaData.put("roles", "");
        metaData.put("links", "");

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setClassification(StoredDocumentHalResource.ClassificationEnum.PUBLIC);
        storedDocumentHalResource.setCreatedBy(USER_ID);
        storedDocumentHalResource.setCreatedOn(time);
        storedDocumentHalResource.setModifiedOn(time);
        storedDocumentHalResource.setTtl(time);
        storedDocumentHalResource.setLastModifiedBy(USER_ID);
        storedDocumentHalResource.setMetadata(metaData);

        return storedDocumentHalResource;
    }

}
