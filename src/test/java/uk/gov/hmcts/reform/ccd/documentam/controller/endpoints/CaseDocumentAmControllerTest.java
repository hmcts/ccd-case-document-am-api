package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.GeneratedHashCodeResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentMetaDataResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_PERMISSION_ERROR;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.USER_PERMISSION_ERROR;

public class CaseDocumentAmControllerTest implements TestFixture {
    private static final String VALID_RESPONSE = "Valid Response from API";
    private static final String RESPONSE_CODE = "Status code is OK";
    private static final String NO_CONTENT_RESPONSE_CODE = "Status code is No Content";
    private static final String FORBIDDEN = "forbidden";
    private static final String TEST_S2S_TOKEN = "Test s2sToken";

    private static final Document DOCUMENT = Document.builder()
        .originalDocumentName("test.png")
        .build();

    private CaseDocumentAmController testee;

    @Mock
    private DocumentManagementService documentManagementService;
    @Mock
    private CaseDataStoreService caseDataStoreService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testee = new CaseDocumentAmController(documentManagementService, securityUtils);
        when(securityUtils.getServiceNameFromS2SToken(TEST_S2S_TOKEN)).thenReturn(XUI_WEBAPP);
        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
    }

    @Test
    void shouldGetValidMetaDataResponse() {
        doNothing().when(documentManagementService)
            .checkUserPermission(
                DOCUMENT.getCaseId(),
                MATCHED_DOCUMENT_ID,
                Permission.READ,
                USER_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService)
            .checkServicePermission(
                DOCUMENT.getCaseTypeId(),
                DOCUMENT.getJurisdictionId(),
                XUI_WEBAPP,
                Permission.READ,
                SERVICE_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString());

        final ResponseEntity<Document> response = testee.getDocumentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN);

        assertAll(
            () -> assertNotNull(response, "Valid Response from API"),
            () -> assertNotNull(response.getBody(), "Valid response body"),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK")
        );
    }

    @Test
    void shouldNotGetValidMetaDataResponseWhenServiceNotAuthorised() {
        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.READ,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.getDocumentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN));
    }

    @Test
    void shouldNotGetValidMetaDataResponseWhenUserNotAuthorised() {
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.READ,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.getDocumentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN));
    }

    @Test
    @DisplayName("should get 200 document binary content")
    void shouldGetDocumentBinaryContent() {
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.READ,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        doReturn(setDocumentBinaryContent("OK"))
            .when(documentManagementService).getDocumentBinaryContent(MATCHED_DOCUMENT_ID);

        ResponseEntity<ByteArrayResource> response =
            testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN);

        assertAll(
            () -> verify(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID),
            () -> verify(documentManagementService, times(1)).getDocumentBinaryContent(MATCHED_DOCUMENT_ID),
            () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
            () -> assertNotNull(response.getBody())
        );
    }

    @Test
    @DisplayName("should throw 403 forbidden  when the requested document does not have read permission")
    void shouldThrowForbiddenWhenDocumentDoesNotHaveReadPermission() {
        doNothing().when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        doThrow(ForbiddenException.class).when(documentManagementService)
            .getDocumentBinaryContent(MATCHED_DOCUMENT_ID);

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN));
    }


    @Test
    @DisplayName("should throw 403 forbidden when the requested document does not match with available doc")
    void shouldThrowForbiddenWhenDocumentDoesNotMatch() {
        Optional<DocumentPermissions> documentPermissions = Optional.of(getDocumentPermissions(
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doReturn(documentPermissions).when(caseDataStoreService)
            .getCaseDocumentMetadata(CASE_ID_VALUE, MATCHED_DOCUMENT_ID);
        doThrow(ForbiddenException.class).when(documentManagementService)
            .getDocumentBinaryContent(MATCHED_DOCUMENT_ID);
        doNothing().when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN));
    }

    @Test
    @DisplayName("should throw 403 forbidden when the service is not authorised to access")
    void shouldThrowForbiddenWhenServiceIsNotAuthorised() {
        Optional<DocumentPermissions> documentPermissions = Optional.of(getDocumentPermissions(
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doReturn(documentPermissions).when(caseDataStoreService)
            .getCaseDocumentMetadata(CASE_ID_VALUE, MATCHED_DOCUMENT_ID);
        doReturn(setDocumentBinaryContent(FORBIDDEN)).when(documentManagementService)
            .getDocumentBinaryContent(MATCHED_DOCUMENT_ID);
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN));
    }

    @Test
    @DisplayName("should get 204 when document delete is successful")
    void shouldDeleteDocumentByDocumentId() {
        doNothing().when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.UPDATE,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.UPDATE,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService).deleteDocument(MATCHED_DOCUMENT_ID, true);

        final ResponseEntity<Void> response = testee.deleteDocumentByDocumentId(MATCHED_DOCUMENT_ID,
                                                                                true,
                                                                                TEST_S2S_TOKEN);

        assertAll(
            () -> assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), NO_CONTENT_RESPONSE_CODE)
        );
    }

    @Test
    @DisplayName("should get 403 when service is not authorised")
    void shouldNotAllowDeleteDocumentByDocumentIdWhenServiceIsNotAuthorised() {
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.UPDATE,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.UPDATE,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());

        doNothing().when(documentManagementService).deleteDocument(MATCHED_DOCUMENT_ID, true);

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.deleteDocumentByDocumentId(MATCHED_DOCUMENT_ID, true, TEST_S2S_TOKEN));
    }

    @Test
    void shouldPatchDocumentByDocumentId() {
        doNothing().when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.UPDATE,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.UPDATE,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());

        final UpdateTtlRequest body = new UpdateTtlRequest(null);
        PatchDocumentResponse patchDocumentResponse = new PatchDocumentResponse();
        patchDocumentResponse.setOriginalDocumentName("test.png");
        doReturn(new ResponseEntity<>(patchDocumentResponse, HttpStatus.OK))
            .when(documentManagementService).patchDocument(MATCHED_DOCUMENT_ID, body);

        final ResponseEntity<PatchDocumentResponse> response = testee.patchDocumentByDocumentId(
            MATCHED_DOCUMENT_ID,
            body,
            TEST_S2S_TOKEN
        );
        assertAll(
            () -> assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE)
        );
    }

    @Test
    void shouldNotAllowPatchDocumentByDocumentIdWhenServiceIsNotAuthorised() {
        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(DOCUMENT_ID);
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.UPDATE,
                                    SERVICE_PERMISSION_ERROR,
                                    DOCUMENT_ID.toString());
        final UpdateTtlRequest body = new UpdateTtlRequest();

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.patchDocumentByDocumentId(DOCUMENT_ID, body, TEST_S2S_TOKEN));
    }

    @Test
    void shouldNotPatchMetaDataOnDocumentsWhenServiceIsNotAuthorised() {
        doThrow(ForbiddenException.class).when(documentManagementService).checkServicePermission(
            anyString(),
            anyString(),
            anyString(),
            eq(Permission.ATTACH),
            eq(SERVICE_PERMISSION_ERROR),
            anyString()
        );
        final CaseDocumentsMetadata body = CaseDocumentsMetadata.builder()
            .caseTypeId("")
            .jurisdictionId("")
            .build();

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.patchMetaDataOnDocuments(body, TEST_S2S_TOKEN));
    }

    @Test
    void shouldPatchMetaDataOnDocuments() {
        doNothing().when(documentManagementService)
            .checkServicePermission(eq(DOCUMENT.getCaseTypeId()),
                                    eq(DOCUMENT.getJurisdictionId()),
                                    eq(XUI_WEBAPP),
                                    eq(Permission.ATTACH),
                                    eq(SERVICE_PERMISSION_ERROR),
                                    anyString());
        DocumentHashToken document = DocumentHashToken.builder()
            .id(UUID.fromString("cab18c21-8b7c-452b-937c-091225e0cc12"))
            .build();
        CaseDocumentsMetadata body = CaseDocumentsMetadata.builder()
            .caseId("1111122222333334")
            .documentHashTokens(Collections.singletonList(document))
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .build();

        final ResponseEntity<PatchDocumentMetaDataResponse> response = testee.patchMetaDataOnDocuments(
            body,
            TEST_S2S_TOKEN
        );

        assertAll(
            () -> assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE),
            () -> verify(documentManagementService).checkServicePermission(eq(CASE_TYPE_ID_VALUE),
                                                                           eq(JURISDICTION_ID_VALUE),
                                                                           eq(XUI_WEBAPP),
                                                                           eq(Permission.ATTACH),
                                                                           eq(SERVICE_PERMISSION_ERROR),
                                                                           anyString())
        );
    }

    @Test
    @DisplayName("Should go through happy path")
    void uploadDocuments_HappyPath() {

        UploadResponse mockResponse = new UploadResponse(List.of(Document.builder().build()));

        doNothing().when(documentManagementService).checkServicePermission(
            eq(CASE_TYPE_ID_VALUE),
            eq(JURISDICTION_ID_VALUE),
            eq(XUI_WEBAPP),
            eq(Permission.CREATE),
            eq(SERVICE_PERMISSION_ERROR),
            anyString()
        );
        List<MultipartFile> multipartFiles = generateMultipartList();
        doReturn(mockResponse).when(documentManagementService).uploadDocuments(
            multipartFiles,
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
                JURISDICTION_ID_VALUE
        );

        final DocumentUploadRequest documentUploadRequest = new DocumentUploadRequest(
            multipartFiles,
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
                JURISDICTION_ID_VALUE
        );

        UploadResponse finalResponse = testee.uploadDocuments(
            documentUploadRequest,
            bindingResult,
            TEST_S2S_TOKEN
        );

        assertEquals(finalResponse, mockResponse);
    }

    @Test
    void testShouldRaiseExceptionWhenBindingResultHasErrorsDuringUploadDocuments() {
        doReturn(true).when(bindingResult).hasErrors();

        final DocumentUploadRequest documentUploadRequest = new DocumentUploadRequest(
            generateMultipartList(),
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
                JURISDICTION_ID_VALUE
        );

        assertThatExceptionOfType(BadRequestException.class)
            .isThrownBy(() -> testee.uploadDocuments(documentUploadRequest, bindingResult, TEST_S2S_TOKEN));
    }

    private DocumentPermissions getDocumentPermissions(List<Permission> permission) {
        return DocumentPermissions.builder()
            .permissions(permission)
            .id(UNMATCHED_DOCUMENT_ID)
            .build();
    }

    private ResponseEntity<ByteArrayResource> setDocumentBinaryContent(String responseType) {
        if (responseType.equals("OK")) {
            return new ResponseEntity<>(
                new ByteArrayResource("test document content".getBytes()),
                getHttpHeaders(),
                HttpStatus.OK
            );
        } else if (responseType.equals(FORBIDDEN)) {
            return new ResponseEntity<>(
                new ByteArrayResource("".getBytes()),
                getHttpHeaders(),
                HttpStatus.FORBIDDEN
            );
        }

        return new ResponseEntity<>(new ByteArrayResource("".getBytes()), HttpStatus.OK);
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("OriginalFileName", "OriginalFileName");
        headers.set("Content-Disposition", "Content-Disposition");
        headers.set("data-source", "data-source");
        headers.set("Content-Length", "25");
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private List<MultipartFile> generateMultipartList() {
        final String contentType = "image/jpeg";

        return List.of(
            new MockMultipartFile("file1", "test1.jpg", contentType, "HelloString".getBytes()),
            new MockMultipartFile("file2", "test2.jpg", contentType, "HelloString2".getBytes())
        );
    }

    @Test
    void generateHashCode_HappyPath() {

        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doNothing().when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.HASHTOKEN,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        when(documentManagementService.generateHashToken(MATCHED_DOCUMENT_ID))
            .thenReturn("hashToken");

        final ResponseEntity<GeneratedHashCodeResponse> responseEntity =
            testee.generateHashCode(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("hashToken", responseEntity.getBody().getHashToken());
    }

    @Test
    //this test returns an illegal argument exception because UUID.fromString() contains a throw for illegal arguments
    void generateHashCode_BadRequest() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> testee.generateHashCode(UUID.fromString("A.A"), TEST_S2S_TOKEN));
    }

    @Test
    void generateHashCode_BadRequestWhenServiceIsNotAuthorised() {

        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    XUI_WEBAPP,
                                    Permission.HASHTOKEN,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        when(documentManagementService.generateHashToken(MATCHED_DOCUMENT_ID))
            .thenReturn("hashToken");

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.generateHashCode(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN));
    }

}
