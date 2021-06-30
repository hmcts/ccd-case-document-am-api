package uk.gov.hmcts.reform.ccd.documentam.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.AuditOperationType;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.documentam.util.ApplicationUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CLASSIFICATION;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.CASE_ID_VALUE;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.DOCUMENT_ID;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.getJsonString;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDeleteDocumentByDocumentId;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentBinaryContent;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentManagementUploadDocument;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentUrlNoPermissions;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentUrlWithReadPermissions;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubGetDocumentMetaData;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubPatchDocument;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubPatchDocumentMetaData;

public class CaseDocumentAmControllerIT extends BaseTest {

    @Value("${idam.s2s-auth.totp_secret}")
    protected String salt;

    @Autowired
    private MockMvc mockMvc;

    public static final String RESPONSE_RESULT_KEY = "Result";
    public static final String RESPONSE_ERROR_KEY = "errorCode";

    public static final String SUCCESS = "Success";
    public static final int ERROR_403 = 403;

    private static final String MAIN_URL = "/cases/documents";
    private static final String ATTACH_TO_CASE_URL = "/attachToCase";
    private static final String SERVICE_NAME_XUI_WEBAPP = "xui_webapp";
    private static final String SERVICE_NAME_CCD_DATA = "ccd_data";

    private static final String CLASSIFICATION_VALUE = "PUBLIC";
    private static final String CASE_TYPE_ID_VALUE = "BEFTA_CASETYPE_2";
    private static final String CASE_TYPE_ID_MOVING_CASE_VALUE = "CMC_ExceptionRecord";
    private static final String JURISDICTION_ID_VALUE = "BEFTA_JURISDICTION_2";
    private static final String META_DATA_JSON_EXPRESSION = "$.metadata.";


    private static final String USER_ID = "d5566a63-f87c-4658-a4d6-213d949f8415";

    @Test
    void shouldSuccessfullyUploadDocument() throws Exception {

        stubDocumentManagementUploadDocument();

        MockMultipartFile firstFile =
            new MockMultipartFile("files", "filename.txt",
                                  "text/plain", "some xml".getBytes());
        MockMultipartFile secondFile =
            new MockMultipartFile("data", "other-file-name.data",
                                  "text/plain", "some other type".getBytes());
        MockMultipartFile jsonFile =
            new MockMultipartFile("json", "",
                                  MediaType.APPLICATION_JSON_VALUE, "{\"json\": \"someValue\"}".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart(MAIN_URL)
                            .file(firstFile)
                            .file(secondFile)
                            .file(jsonFile)
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP))
                            .param(CLASSIFICATION, CLASSIFICATION_VALUE)
                            .param(CASE_TYPE_ID, CASE_TYPE_ID_VALUE)
                            .param(JURISDICTION_ID, JURISDICTION_ID_VALUE)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
            .andExpect(status().isOk())

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.UPLOAD_DOCUMENTS,
                SERVICE_NAME_XUI_WEBAPP,
                null,
                null));
    }


    @Test
    void shouldSuccessfullyGetDocumentByDocumentId() throws Exception {
        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResource();

        stubDocumentUrlWithReadPermissions();
        stubGetDocumentMetaData(storedDocumentResource);

        ArrayList<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());
        mockMvc.perform(get(MAIN_URL + "/" +  DOCUMENT_ID)
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP)))
            .andExpect(status().isOk())
            .andExpect(jsonPath(META_DATA_JSON_EXPRESSION + CASE_ID, is(CASE_ID_VALUE)))
            .andExpect(jsonPath(META_DATA_JSON_EXPRESSION + CASE_TYPE_ID, is(CASE_TYPE_ID_VALUE)))
            .andExpect(jsonPath(META_DATA_JSON_EXPRESSION + JURISDICTION_ID, is(JURISDICTION_ID_VALUE)))
            .andExpect(jsonPath("$._links.self.href",
                                is("http://localhost" + MAIN_URL + "/" + DOCUMENT_ID)))

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.DOWNLOAD_DOCUMENT_BY_ID,
                SERVICE_NAME_XUI_WEBAPP,
                documentIds,
                null));
    }

    @Test
    void shouldSuccessfullyDeleteDocumentByDocumentId() throws Exception {
        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResource();

        stubGetDocumentMetaData(storedDocumentResource);
        stubDeleteDocumentByDocumentId();

        List<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());

        mockMvc.perform(delete(MAIN_URL + "/" + DOCUMENT_ID)
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP)))
            .andExpect(status().isNoContent())

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.DELETE_DOCUMENT_BY_DOCUMENT_ID,
                SERVICE_NAME_XUI_WEBAPP,
                documentIds,
                null));
    }

    @Test
    void shouldSuccessfullyGetDocumentBinaryContent() throws Exception {
        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResource();

        stubDocumentUrlWithReadPermissions();
        stubGetDocumentMetaData(storedDocumentResource);
        stubDocumentBinaryContent();

        List<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());

        mockMvc.perform(get(MAIN_URL + "/" + DOCUMENT_ID + "/binary")
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP)))
            .andExpect(status().isOk())

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.DOWNLOAD_DOCUMENT_BINARY_CONTENT_BY_ID,
                SERVICE_NAME_XUI_WEBAPP,
                documentIds,
                null));
    }

    @Test
    void shouldSuccessfullyPatchDocumentByDocumentId() throws Exception {
        UpdateDocumentCommand body = new UpdateDocumentCommand();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        String formattedTTL = dateFormat.format(new Timestamp(new Date().getTime() + Long.parseLong("600000")));
        body.setTtl(formattedTTL);

        Date time = Date.from(Instant.now());

        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResourceToUpdatePatch(time);

        stubGetDocumentMetaData(storedDocumentResource);
        stubPatchDocument(storedDocumentResource);

        List<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());

        mockMvc.perform(patch(MAIN_URL + "/" + DOCUMENT_ID)
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP))
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(getJsonString(body)))
            .andExpect(status().isOk())

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.PATCH_DOCUMENT_BY_DOCUMENT_ID,
                SERVICE_NAME_XUI_WEBAPP,
                documentIds,
                null));
    }

    @Test
    void shouldSuccessfullyPatchMetaDataOnDocument() throws Exception {
        String hashToken = ApplicationUtils
            .generateHashCode(salt.concat(DOCUMENT_ID.toString()
                                              .concat(CASE_ID_VALUE)
                                              .concat(JURISDICTION_ID_VALUE)
                                              .concat(CASE_TYPE_ID_MOVING_CASE_VALUE)));

        List<DocumentHashToken> documentHashTokens = new ArrayList<>();
        documentHashTokens.add(new DocumentHashToken(DOCUMENT_ID.toString(), hashToken));
        CaseDocumentsMetadata body = new CaseDocumentsMetadata();
        body.setDocumentHashTokens(documentHashTokens);
        body.setCaseId(CASE_ID_VALUE);
        body.setCaseTypeId(CASE_TYPE_ID_MOVING_CASE_VALUE);
        body.setJurisdictionId(JURISDICTION_ID_VALUE);

        Date time = Date.from(Instant.now());

        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResourceToUpdatePatch(time);

        stubGetDocumentMetaData(storedDocumentResource);
        stubPatchDocumentMetaData(storedDocumentResource);

        List<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());

        mockMvc.perform(patch(MAIN_URL + ATTACH_TO_CASE_URL)
                                                          .headers(createHttpHeaders(SERVICE_NAME_CCD_DATA))
                                                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                          .content(getJsonString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath(RESPONSE_RESULT_KEY, is(SUCCESS)))

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.PATCH_METADATA_ON_DOCUMENTS,
                SERVICE_NAME_CCD_DATA,
                documentIds,
                body.getCaseId()
            ));
    }

    @Test
    void shouldNotSuccessfullyPatchMetaDataWhenDocumentIsNotMovingCases() throws Exception {
        String hashToken = ApplicationUtils
            .generateHashCode(salt.concat(DOCUMENT_ID.toString()
                                              .concat(CASE_ID_VALUE)
                                              .concat(JURISDICTION_ID_VALUE)
                                              .concat(CASE_TYPE_ID_VALUE)));

        List<DocumentHashToken> documentHashTokens = new ArrayList<>();
        documentHashTokens.add(new DocumentHashToken(DOCUMENT_ID.toString(), hashToken));
        CaseDocumentsMetadata body = new CaseDocumentsMetadata();
        body.setDocumentHashTokens(documentHashTokens);
        body.setCaseId(CASE_ID_VALUE);
        body.setCaseTypeId(CASE_TYPE_ID);
        body.setJurisdictionId(JURISDICTION_ID_VALUE);

        Date time = Date.from(Instant.now());

        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResourceToUpdatePatch(time);
        Map<String, String> updatedMetaData = storedDocumentResource.getMetadata();
        updatedMetaData.put(CASE_TYPE_ID, CASE_TYPE_ID_MOVING_CASE_VALUE);
        storedDocumentResource.setMetadata(updatedMetaData);

        stubGetDocumentMetaData(storedDocumentResource);
        stubPatchDocumentMetaData(storedDocumentResource);

        List<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());

        mockMvc.perform(patch(MAIN_URL + ATTACH_TO_CASE_URL)
                            .headers(createHttpHeaders(SERVICE_NAME_CCD_DATA))
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(getJsonString(body)))
            .andExpect(status().isForbidden())

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.PATCH_METADATA_ON_DOCUMENTS,
                SERVICE_NAME_CCD_DATA,
                documentIds,
                body.getCaseId()
            ));
    }

    @Test
    void shouldBeForbiddenWhenPatchingMetaDataOnDocumentWithIncorrectDocumentHashToken() throws Exception {
        List<DocumentHashToken> documentHashTokens = new ArrayList<>();
        documentHashTokens.add(new DocumentHashToken(DOCUMENT_ID.toString(), "567890976546789"));
        CaseDocumentsMetadata body = new CaseDocumentsMetadata();
        body.setDocumentHashTokens(documentHashTokens);
        body.setCaseId(CASE_ID_VALUE);
        body.setCaseTypeId(CASE_TYPE_ID_VALUE);
        body.setJurisdictionId(JURISDICTION_ID_VALUE);

        Date time = Date.from(Instant.now());

        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResourceToUpdatePatch(time);

        stubGetDocumentMetaData(storedDocumentResource);
        stubPatchDocumentMetaData(storedDocumentResource);

        List<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());

        mockMvc.perform(patch(MAIN_URL + ATTACH_TO_CASE_URL)
                            .headers(createHttpHeaders(SERVICE_NAME_CCD_DATA))
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(getJsonString(body)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath(RESPONSE_ERROR_KEY, is(ERROR_403)))

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.PATCH_METADATA_ON_DOCUMENTS,
                SERVICE_NAME_CCD_DATA,
                documentIds,
                body.getCaseId()));
    }

    @Test
    void shouldFailToUploadDocumentEmptyFile() throws Exception {

        stubDocumentManagementUploadDocument();

        MockMultipartFile jsonFile1 =
            new MockMultipartFile("name", null,
                                  null, new byte[0]);

        mockMvc.perform(MockMvcRequestBuilders.multipart(MAIN_URL)
                            .file(jsonFile1)
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP))
                            .param(CLASSIFICATION, CLASSIFICATION_VALUE)
                            .param(CASE_TYPE_ID, CASE_TYPE_ID_VALUE)
                            .param(JURISDICTION_ID, JURISDICTION_ID_VALUE)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
            .andExpect(status().isInternalServerError())

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.UPLOAD_DOCUMENTS,
                SERVICE_NAME_XUI_WEBAPP,
                null,
                null));
    }

    @Test
    void shouldBeForbiddenGetDocumentByDocumentIdWithNoPermissions() throws Exception {
        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResource();

        stubDocumentUrlNoPermissions();
        stubGetDocumentMetaData(storedDocumentResource);

        ArrayList<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());
        mockMvc.perform(get(MAIN_URL + "/" +  DOCUMENT_ID)
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP)))
            .andExpect(status().isForbidden())

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.DOWNLOAD_DOCUMENT_BY_ID,
                SERVICE_NAME_XUI_WEBAPP,
                documentIds,
                null));
    }

    @Test
    void shouldBeForbiddenWhenGettingDocumentBinaryContentWithNoPermissions() throws Exception {
        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResource();

        stubDocumentUrlNoPermissions();
        stubGetDocumentMetaData(storedDocumentResource);
        stubDocumentBinaryContent();

        List<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());

        mockMvc.perform(get(MAIN_URL + "/" + DOCUMENT_ID + "/binary")
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP)))
            .andExpect(status().isForbidden())

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.DOWNLOAD_DOCUMENT_BINARY_CONTENT_BY_ID,
                SERVICE_NAME_XUI_WEBAPP,
                documentIds,
                null));
    }

    @Test
    void shouldNotFindDocumentToDeleteWhenTryingToDeleteDocumentByDocumentId() throws Exception {
        UUID random = UUID.randomUUID();

        StoredDocumentHalResource storedDocumentResource = getStoredDocumentResource();

        stubGetDocumentMetaData(storedDocumentResource);
        stubDeleteDocumentByDocumentId();

        List<String> documentIds = new ArrayList<>();
        documentIds.add(random.toString());

        mockMvc.perform(delete(MAIN_URL + "/" + random)
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP)))
            .andExpect(status().isNotFound())

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.DELETE_DOCUMENT_BY_DOCUMENT_ID,
                SERVICE_NAME_XUI_WEBAPP,
                documentIds,
                null));
    }


    private StoredDocumentHalResource getStoredDocumentResource() {

        Map<String, String> metaData = new HashMap<>();
        metaData.put(CASE_ID, CASE_ID_VALUE);
        metaData.put(CASE_TYPE_ID, CASE_TYPE_ID_VALUE);
        metaData.put(JURISDICTION_ID, JURISDICTION_ID_VALUE);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setClassification(StoredDocumentHalResource.ClassificationEnum.PUBLIC);
        storedDocumentHalResource.setCreatedBy(USER_ID);
        storedDocumentHalResource.setCreatedOn(Date.from(Instant.now()));
        storedDocumentHalResource.setLastModifiedBy(USER_ID);
        storedDocumentHalResource.setMetadata(metaData);

        return storedDocumentHalResource;
    }

    private StoredDocumentHalResource getStoredDocumentResourceToUpdatePatch(Date time) {
        Map<String, String> metaData = new HashMap<>();
        metaData.put("size", "10");
        metaData.put("createdBy", USER_ID);
        metaData.put(CASE_ID, CASE_ID_VALUE);
        metaData.put(CASE_TYPE_ID, CASE_TYPE_ID_MOVING_CASE_VALUE);
        metaData.put(JURISDICTION_ID, JURISDICTION_ID_VALUE);
        metaData.put(CLASSIFICATION, CLASSIFICATION_VALUE);
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
