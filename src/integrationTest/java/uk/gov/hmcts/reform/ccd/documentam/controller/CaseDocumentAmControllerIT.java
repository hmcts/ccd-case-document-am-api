package uk.gov.hmcts.reform.ccd.documentam.controller;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
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

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.DOCUMENT_ID;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.getJsonString;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDeleteDocumentByDocumentId;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentBinaryContent;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentManagementUploadDocument;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentUrl;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubGetDocumentMetaData;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubPatchDocument;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubPatchDocumentMetaData;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubUploadDocument;

@RunWith(SpringRunner.class)

public class CaseDocumentAmControllerIT extends BaseTest {

    @Value("${idam.s2s-auth.totp_secret}")
    protected String salt;

    @Autowired
    private MockMvc mockMvc;

    private static final String MAIN_URL = "/cases/documents";
    private static final String ATTACH_TO_CASE_URL = "/attachToCase";
    private static final String SERVICE_NAME_XUI_WEBAPP = "xui_webapp";
    private static final String SERVICE_NAME_CCD_DATA = "ccd_data";

    private static final String CASE_ID_KEY = "caseId";
    private static final String CLASSIFICATION_KEY = "classification";
    private static final String CASE_TYPE_ID_KEY = "caseTypeId";
    private static final String JURISDICTION_ID_KEY = "jurisdictionId";

    private static final String CLASSIFICATION = "PUBLIC";
    private static final String CASE_TYPE_ID = "BEFTA_CASETYPE_2";
    private static final String JURISDICTION_ID = "BEFTA_JURISDICTION_2";

    private static final String CASE_ID = "1584722156538291";
    private static final String USER_ID = "d5566a63-f87c-4658-a4d6-213d949f8415";

    @Test
    void shouldSuccessfullyUploadDocument() throws Exception {

        stubDocumentUrl();
        stubUploadDocument();
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
                            .param(CLASSIFICATION_KEY, CLASSIFICATION)
                            .param(CASE_TYPE_ID_KEY, CASE_TYPE_ID)
                            .param(JURISDICTION_ID_KEY, JURISDICTION_ID)
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

        stubDocumentUrl();
        stubGetDocumentMetaData(storedDocumentResource);

        ArrayList<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());
        String metaDataJsonExpression = "$.metadata.";
        mockMvc.perform(get(MAIN_URL + "/" +  DOCUMENT_ID)
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP)))
            .andExpect(status().isOk())
            .andExpect(jsonPath(metaDataJsonExpression + CASE_ID_KEY, is(CASE_ID)))
            .andExpect(jsonPath(metaDataJsonExpression + CASE_TYPE_ID_KEY, is(CASE_TYPE_ID)))
            .andExpect(jsonPath(metaDataJsonExpression + JURISDICTION_ID_KEY, is(JURISDICTION_ID)))
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

        stubDocumentUrl();
        stubGetDocumentMetaData(storedDocumentResource);
        stubDeleteDocumentByDocumentId();

        ArrayList<String> documentIds = new ArrayList<>();
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

        stubDocumentUrl();
        stubGetDocumentMetaData(storedDocumentResource);
        stubDocumentBinaryContent();

        ArrayList<String> documentIds = new ArrayList<>();
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

        stubDocumentUrl();
        stubGetDocumentMetaData(storedDocumentResource);
        stubPatchDocument(storedDocumentResource);

        ArrayList<String> documentIds = new ArrayList<>();
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

        ArrayList<String> documentIds = new ArrayList<>();
        documentIds.add(DOCUMENT_ID.toString());

        mockMvc.perform(patch(MAIN_URL + ATTACH_TO_CASE_URL)
                            .headers(createHttpHeaders(SERVICE_NAME_CCD_DATA))
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(getJsonString(body)))
            .andExpect(status().isOk())

            .andExpect(hasGeneratedLogAudit(
                AuditOperationType.PATCH_METADATA_ON_DOCUMENTS,
                SERVICE_NAME_CCD_DATA,
                documentIds,
                body.getCaseId()));
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

        ArrayList<String> documentIds = new ArrayList<>();
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
                body.getCaseId()));
    }


    private StoredDocumentHalResource getStoredDocumentResource() {

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

    private StoredDocumentHalResource getStoredDocumentResourceToUpdatePatch(Date time) {
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
