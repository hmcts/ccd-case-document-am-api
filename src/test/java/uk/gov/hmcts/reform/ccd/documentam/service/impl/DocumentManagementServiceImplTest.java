package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.client.datastore.CaseDataStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DocumentStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.util.ApplicationUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.buildPatchDocumentResponse;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.buildUpdateDocumentCommand;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_JURISDICTION_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.ORIGINAL_FILE_NAME;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_PERMISSION_ERROR;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.USER_PERMISSION_ERROR;

@ExtendWith(MockitoExtension.class)
class DocumentManagementServiceImplTest implements TestFixture {

    private static final String BULK_SCAN_PROCESSOR = "bulk_scan_processor";

    @Mock
    private DocumentStoreClient documentStoreClient;

    @Mock
    private CaseDataStoreClient caseDataStoreServiceMock;

    @Mock
    private AuthorisedServices authorisedServicesMock;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private DocumentManagementServiceImpl sut;

    private final List<String> bulkScanExceptionRecordTypes = Arrays.asList(
        "CMC_ExceptionRecord",
        "FINREM_ExceptionRecord",
        "SSCS_ExceptionRecord",
        "PROBATE_ExceptionRecord",
        "PUBLICLAW_ExceptionRecord",
        "DIVORCE_ExceptionRecord"
    );

    @Test
    void documentMetadataInstantiation() {
        assertNotNull(sut);
    }

    @Test
    void getDocumentMetadata_HappyPath() {
        // GIVEN
        final Document expectedDocument = Document.builder().build();
        stubGetDocument(expectedDocument);

        // WHEN
        final Document actualDocument = sut.getDocumentMetadata(DOCUMENT_ID);

        // THEN
        assertThat(actualDocument)
            .isNotNull()
            .isEqualTo(expectedDocument);

        verify(documentStoreClient).getDocument(DOCUMENT_ID);
    }

    @Test
    void getDocumentMetadata_not_found() {
        final ResourceNotFoundException resourceNotFoundException =
            new ResourceNotFoundException(RANDOM_STRING,
                                          new HttpClientErrorException(HttpStatus.NOT_FOUND));

        doReturn(Either.left(resourceNotFoundException))
            .when(documentStoreClient).getDocument(DOCUMENT_ID);

        assertThatExceptionOfType(ResourceNotFoundException.class)
            .isThrownBy(() -> sut.getDocumentMetadata(DOCUMENT_ID));

        verify(documentStoreClient).getDocument(DOCUMENT_ID);
    }

    @Test
    void getDocumentBinaryContent_HappyPath() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ORIGINAL_FILE_NAME, "name");
        headers.add(CONTENT_DISPOSITION, "disp");
        headers.add(DATA_SOURCE, "source");
        headers.add(CONTENT_TYPE, "type");

        doReturn(new ResponseEntity<>(headers, HttpStatus.OK))
            .when(documentStoreClient).getDocumentAsBinary(any(UUID.class));

        final ResponseEntity<ByteArrayResource> responseEntity = sut.getDocumentBinaryContent(DOCUMENT_ID);

        assertThat(responseEntity)
            .isNotNull()
            .satisfies(entity -> {
                assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(entity.getHeaders()).isEqualTo(headers);
            });

        verify(documentStoreClient).getDocumentAsBinary(DOCUMENT_ID);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void checkServicePermission_WhenJurisdictionIdIsNull() {
        final String jurisdictionId = null;

        mockAuthorisedServices();

        assertThrows(NullPointerException.class, () ->
            sut.checkServicePermission(
                "BEFTA_CASETYPE_1_1",
                jurisdictionId,
                SERVICE_NAME_XUI_WEBAPP,
                Permission.READ,
                "log string",
                "exception string"));
    }

    @Test
    void checkServicePermission_CaseTypeIdExistsButNotAuthorised() {
        assertThrows(ForbiddenException.class, () ->
            sut.checkServicePermission(
                "randomCaseTypeId",
                "juridiction",
                SERVICE_NAME_XUI_WEBAPP,
                Permission.READ,
                "log string",
                "exception string"
            ));
    }

    @Test
    void checkServicePermissionForUpload_WhenServiceIsNotAuthorised() {
        assertThrows(ForbiddenException.class, () -> sut.checkServicePermission(
            "caseTypeId",
            JURISDICTION_ID_VALUE,
            BULK_SCAN_PROCESSOR,
            Permission.READ,
            SERVICE_PERMISSION_ERROR,
            "exception message"
        ));
    }

    @Test
    void checkServicePermissionForUpload_WhenCaseTypeIsNull() {
        mockAuthorisedServices();

        assertThrows(ForbiddenException.class, () -> sut.checkServicePermission(
            "",
            JURISDICTION_ID_VALUE,
            BULK_SCAN_PROCESSOR,
            Permission.READ,
            SERVICE_PERMISSION_ERROR,
            "exception message"
        ));
    }

    @Test
    void checkServicePermissionForUpload_WhenJurisdictionIdIsNull() {
        mockAuthorisedServices();

        assertThrows(ForbiddenException.class, () -> sut.checkServicePermission(
            "caseTypeId",
            "",
            BULK_SCAN_PROCESSOR,
            Permission.READ,
            SERVICE_PERMISSION_ERROR,
            "exception message"
        ));
    }

    @Test
    void checkServicePermission_WhenServiceIdIsNotAuthorised() {
        assertThrows(ForbiddenException.class, () -> sut.checkServicePermission(
            CASE_TYPE_ID_VALUE,
            JURISDICTION_ID_VALUE,
            "bad_Service_name",
            Permission.READ,
            "log string",
            "exception string"));
    }

    @Test
    void checkUserPermission_Throws_CaseNotFoundException() {
        final Document document = buildDocument("1234567890123456", "", "");

        when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(null);

        assertThrows(Exception.class, () -> sut.checkUserPermission(
            document.getCaseId(),
            DOCUMENT_ID,
            Permission.READ,
            USER_PERMISSION_ERROR,
            "exception string"
        ));

        verifyCaseDataServiceGetDocMetadata();
    }

    @Test
    void checkUserPermission_ReturnsFalse_Scenario1() {
        final Document document = buildDocument("1234567890123456", "", "");

        final List<Permission> permissionsList = emptyList();
        final DocumentPermissions doc = DocumentPermissions.builder()
            .id(DOCUMENT_ID)
            .permissions(permissionsList)
            .build();
        when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(Optional.of(doc));

        assertThrows(ForbiddenException.class, () -> sut.checkUserPermission(document.getCaseId(),
                                                                             DOCUMENT_ID,
                                                                             Permission.READ,
                                                                             USER_PERMISSION_ERROR,
                                                                             "exception string"));

        verifyCaseDataServiceGetDocMetadata();
    }


    @Test
    void checkUserPermission_ReturnsFalse_Scenario2() {
        final Document document = buildDocument("1234567890123456", "", "");

        final List<Permission> permissionsList = emptyList();
        final DocumentPermissions doc = DocumentPermissions.builder()
            .id(UUID.fromString("40000a2b-00ce-00eb-0068-2d00a700be9c"))
            .permissions(permissionsList)
            .build();
        when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(Optional.of(doc));

        assertThrows(ForbiddenException.class, () -> sut.checkUserPermission(document.getCaseId(),
                                                                             DOCUMENT_ID,
                                                                             Permission.READ,
                                                                             USER_PERMISSION_ERROR,
                                                                             "exception string"));

        verifyCaseDataServiceGetDocMetadata();
    }

    private void stubGetSalt() {
        doReturn("AAAOA7A2AA6AAAA5").when(applicationParams).getSalt();
    }

    private void stubGetDocument(final Document document) {
        doReturn(Either.right(document)).when(documentStoreClient).getDocument(DOCUMENT_ID);
    }

    private void verifyCaseDataServiceGetDocMetadata() {
        verify(caseDataStoreServiceMock, times(1))
            .getCaseDocumentMetadata(anyString(), any(UUID.class));
    }

    @Test
    void patchDocumentMetadata_HappyPath() {
        // GIVEN
        final DocumentHashToken doc = DocumentHashToken.builder()
            .id(DOCUMENT_ID)
            .hashToken(ApplicationUtils.generateHashCode(
                SALT.concat(DOCUMENT_ID.toString())
                    .concat(JURISDICTION_ID_VALUE)
                    .concat(CASE_TYPE_ID_VALUE)))
            .build();

        final Document document = Document.builder()
            .metadata(Map.of(METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE,
                             METADATA_CASE_TYPE_ID, CASE_TYPE_ID_VALUE))
            .build();

        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        final ArgumentCaptor<UpdateDocumentsCommand> updateDocumentsCommandCaptor =
            ArgumentCaptor.forClass(UpdateDocumentsCommand.class);

        stubGetSalt();
        stubGetDocument(document);
        doNothing().when(documentStoreClient).patchDocumentMetadata(updateDocumentsCommandCaptor.capture());

        // WHEN
        sut.patchDocumentMetadata(caseDocumentsMetadata);

        // THEN
        final UpdateDocumentsCommand documentsCommand = updateDocumentsCommandCaptor.getValue();

        assertThat(documentsCommand).isNotNull().satisfies(command -> assertThat(command.getTtl()).isNull());

        verify(documentStoreClient).patchDocumentMetadata(any(UpdateDocumentsCommand.class));
    }

    @Test
    void shouldPatchMetaDataEvenIfTokenIsNotPassed_hashCheckDisabled() {
        // GIVEN
        doReturn(false).when(applicationParams).isHashCheckEnabled();

        final Document document = Document.builder()
            .metadata(Map.of(METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE,
                             METADATA_CASE_TYPE_ID, "CMC_ExceptionRecord"))
            .build();

        final ArgumentCaptor<UpdateDocumentsCommand> updateDocumentsCommandCaptor =
            ArgumentCaptor.forClass(UpdateDocumentsCommand.class);

        stubGetDocument(document);
        doReturn(bulkScanExceptionRecordTypes).when(applicationParams).getMovingCaseTypes();
        doNothing().when(documentStoreClient).patchDocumentMetadata(updateDocumentsCommandCaptor.capture());

        final DocumentHashToken doc = DocumentHashToken.builder().id(DOCUMENT_ID).build();
        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        // WHEN
        sut.patchDocumentMetadata(caseDocumentsMetadata);

        // THEN
        final UpdateDocumentsCommand documentsCommand = updateDocumentsCommandCaptor.getValue();

        assertThat(documentsCommand).isNotNull().satisfies(command -> assertThat(command.getTtl()).isNull());

        verify(documentStoreClient).patchDocumentMetadata(any(UpdateDocumentsCommand.class));
    }

    @Test
    void patchDocumentMetadata_Throws_NotFoundException() {
        // GIVEN
        final DocumentHashToken doc = DocumentHashToken.builder()
            .id(DOCUMENT_ID)
            .hashToken(ApplicationUtils.generateHashCode(
                SALT.concat(DOCUMENT_ID.toString())
                    .concat(JURISDICTION_ID_VALUE)
                    .concat(CASE_TYPE_ID_VALUE)))
            .build();
        final ResourceNotFoundException resourceNotFoundException =
            new ResourceNotFoundException(RANDOM_STRING,
                                          new HttpClientErrorException(HttpStatus.NOT_FOUND));

        doReturn(Either.left(resourceNotFoundException))
            .when(documentStoreClient).getDocument(DOCUMENT_ID);

        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        // WHEN/THEN
        assertThatExceptionOfType(ResourceNotFoundException.class)
            .isThrownBy(() -> sut.patchDocumentMetadata(caseDocumentsMetadata));
        verify(documentStoreClient, never()).patchDocumentMetadata(any(UpdateDocumentsCommand.class));
    }

    @Test
    void shouldNotPatchMetaDataWhenDocumentNotMovingCase_noExceptionRecordType() {
        // GIVEN
        doReturn(false).when(applicationParams).isHashCheckEnabled();

        final Document document = Document.builder()
            .metadata(Map.of(METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE,
                             METADATA_CASE_TYPE_ID, CASE_TYPE_ID_VALUE))
            .build();

        stubGetDocument(document);

        final DocumentHashToken doc = DocumentHashToken.builder().id(DOCUMENT_ID).build();
        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        // WHEN/THEN
        assertThatExceptionOfType(BadRequestException.class)
            .isThrownBy(() -> sut.patchDocumentMetadata(caseDocumentsMetadata));
        verify(documentStoreClient, never()).patchDocumentMetadata(any(UpdateDocumentsCommand.class));
    }

    @Test
    void shouldThrowForbiddenWhenTokenIsNotPassed_hashCheckEnabled() {
        doReturn(true).when(applicationParams).isHashCheckEnabled();

        final DocumentHashToken doc = DocumentHashToken.builder().id(DOCUMENT_ID).build();

        final Document document = Document.builder()
            .metadata(
                Map.of(
                    METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE,
                    METADATA_CASE_TYPE_ID, CASE_TYPE_ID_VALUE
                )
            )
            .build();

        stubGetDocument(document);

        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> sut.patchDocumentMetadata(caseDocumentsMetadata))
            .withMessage("Forbidden: Insufficient permissions: "
                             + "Hash check is enabled but hashToken wasn't provided for the document: "
                             + DOCUMENT_ID);
        verify(documentStoreClient, never()).patchDocumentMetadata(any(UpdateDocumentsCommand.class));
    }

    @Test
    void shouldThrowForbiddenWhenTokenIsNotMatched() {
        final String token = ApplicationUtils.generateHashCode(
            SALT.concat(DOCUMENT_ID.toString())
                .concat(JURISDICTION_ID_VALUE)
                .concat(CASE_TYPE_ID_VALUE));
        final DocumentHashToken doc = DocumentHashToken.builder()
            .id(DOCUMENT_ID)
            .hashToken(token)
            .build();

        final Document document = Document.builder()
            .metadata(Map.of(METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE,
                             METADATA_CASE_TYPE_ID, "DIFFERENT_CASETYPE"))
            .build();

        stubGetSalt();
        stubGetDocument(document);

        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> sut.patchDocumentMetadata(caseDocumentsMetadata))
            .withMessage("Forbidden: Insufficient permissions: Hash token check failed for the document: "
                             + DOCUMENT_ID);
        verify(documentStoreClient, never()).patchDocumentMetadata(any(UpdateDocumentsCommand.class));
    }

    @Test
    void patchDocumentMetadata_Throws_ForbiddenException_InvalidHashToken() {
        final String invalidToken = ApplicationUtils.generateHashCode(
            SALT.concat(DOCUMENT_ID.toString())
                .concat(JURISDICTION_ID_VALUE)
                .concat(CASE_TYPE_ID_VALUE));
        final DocumentHashToken doc = DocumentHashToken.builder()
            .id(DOCUMENT_ID)
            .hashToken(invalidToken)
            .build();

        final Map<String, String> myMetadata = Map.of(
            METADATA_CASE_ID, CASE_ID_VALUE,
            METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE,
            METADATA_CASE_TYPE_ID, CASE_TYPE_ID_VALUE
        );
        final Document document = Document.builder()
            .metadata(myMetadata)
            .build();

        stubGetSalt();
        stubGetDocument(document);

        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> sut.patchDocumentMetadata(caseDocumentsMetadata))
            .withMessage("Forbidden: Insufficient permissions: Hash token check failed for the document: "
                             + DOCUMENT_ID);
        verify(documentStoreClient, never()).patchDocumentMetadata(any(UpdateDocumentsCommand.class));
    }

    @Test
    void uploadDocuments_HappyPath() {
        final Document document = Document.builder()
            .size(1000L)
            .mimeType(MIME_TYPE)
            .originalDocumentName(ORIGINAL_DOCUMENT_NAME)
            .classification(Classification.PUBLIC)
            .links(TestFixture.getLinks())
            .build();

        final DmUploadResponse dmUploadResponse = DmUploadResponse.builder()
            .embedded(DmUploadResponse.Embedded.builder().documents(List.of(document)).build())
            .build();

        stubGetSalt();
        doReturn(dmUploadResponse).when(documentStoreClient).uploadDocuments(any(DocumentUploadRequest.class));

        final String expectedHash = ApplicationUtils
            .generateHashCode(SALT.concat(DOCUMENT_ID.toString()
                                              .concat(JURISDICTION_ID_VALUE)
                                              .concat(CASE_TYPE_ID_VALUE)));

        final DocumentUploadRequest uploadRequest = new DocumentUploadRequest(
            List.of(new MockMultipartFile("afile", "some".getBytes())),
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
            JURISDICTION_ID_VALUE
        );

        final UploadResponse response = sut.uploadDocuments(uploadRequest);

        assertThat(response.getDocuments())
            .hasSize(1)
            .first()
            .satisfies(doc -> {
                assertThat(doc.getClassification()).isEqualTo(Classification.PUBLIC);
                assertThat(doc.getSize()).isEqualTo(1000);
                assertThat(doc.getMimeType()).isEqualTo(MIME_TYPE);
                assertThat(doc.getOriginalDocumentName()).isEqualTo(ORIGINAL_DOCUMENT_NAME);
                assertThat(doc.getHashToken()).isEqualTo(expectedHash);
                assertThat(doc.getLinks().binary.href).isEqualTo(BINARY_LINK);
                assertThat(doc.getLinks().self.href).isEqualTo(SELF_LINK);
            });

        verify(documentStoreClient).uploadDocuments(uploadRequest);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testUploadDocumentsWhenDmUploadResponseIsNull() {
        final DmUploadResponse dmUploadResponse = null;

        doReturn(dmUploadResponse).when(documentStoreClient).uploadDocuments(any(DocumentUploadRequest.class));

        final DocumentUploadRequest uploadRequest = new DocumentUploadRequest(
            List.of(new MockMultipartFile("afile", "some".getBytes())),
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
            JURISDICTION_ID_VALUE
        );

        final UploadResponse response = sut.uploadDocuments(uploadRequest);

        assertThat(response)
            .isNotNull()
            .satisfies(x -> assertThat(x.getDocuments()).isEmpty());

        verify(documentStoreClient).uploadDocuments(uploadRequest);
    }

    @Test
    void patchDocument_HappyPath() {
        // GIVEN
        final UpdateTtlRequest ttlRequest = buildUpdateDocumentCommand();
        final PatchDocumentResponse expectedResponse = buildPatchDocumentResponse();

        doReturn(Either.right(expectedResponse))
            .when(documentStoreClient).patchDocument(any(UUID.class), any(DmTtlRequest.class));

        // WHEN
        final PatchDocumentResponse actualResponse = sut.patchDocument(DOCUMENT_ID, ttlRequest);

        // THEN
        assertThat(actualResponse)
            .isNotNull()
            .isEqualTo(expectedResponse);
        verify(documentStoreClient).patchDocument(eq(DOCUMENT_ID), any(DmTtlRequest.class));
    }

    @Test
    void testShouldRaiseExceptionWhenPatchDocumentFails() {
        // GIVEN
        final UpdateTtlRequest ttlRequest = buildUpdateDocumentCommand();
        final ResourceNotFoundException expectedException =
            new ResourceNotFoundException(RANDOM_STRING, new HttpClientErrorException(HttpStatus.NOT_FOUND));
        doReturn(Either.left(expectedException))
            .when(documentStoreClient).patchDocument(any(UUID.class), any(DmTtlRequest.class));

        // WHEN/THEN
        assertThatExceptionOfType(ResourceNotFoundException.class)
            .isThrownBy(() -> sut.patchDocument(DOCUMENT_ID, ttlRequest));
    }

    @Test
    void testDeleteDocument() {
        final boolean permanent = true;
        doNothing().when(documentStoreClient).deleteDocument(any(UUID.class), anyBoolean());

        sut.deleteDocument(DOCUMENT_ID, permanent);

        verify(documentStoreClient).deleteDocument(DOCUMENT_ID, permanent);
    }

    @Test
    void shouldGenerateHashTokenWhenCaseIdIsPresent() {
        final Document document = buildDocument(CASE_ID_VALUE, "BEFTA_CASETYPE_2_2", JURISDICTION_ID_VALUE);

        stubGetSalt();
        stubGetDocument(document);

        final String result = sut.generateHashToken(DOCUMENT_ID);

        assertNotNull(result);
    }

    @Test
    void shouldGenerateHashTokenWhenCaseIdIsNotPresent() {
        final Document document = Document.builder()
            .metadata(Map.of(METADATA_CASE_TYPE_ID, "BEFTA_CASETYPE_2_2",
                             METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE))
            .build();

        stubGetSalt();
        stubGetDocument(document);

        final String result = sut.generateHashToken(DOCUMENT_ID);

        assertNotNull(result);
    }

    @Test
    void testGenerateHashTokenShouldReturnEmptyStringWhenDocumentIsNotFound() {
        // GIVEN
        final ResourceNotFoundException resourceNotFoundException =
            new ResourceNotFoundException(RANDOM_STRING,
                                          new HttpClientErrorException(HttpStatus.NOT_FOUND));
        doReturn(Either.left(resourceNotFoundException))
            .when(documentStoreClient).getDocument(DOCUMENT_ID);

        // WHEN
        final String result = sut.generateHashToken(DOCUMENT_ID);

        // THEN
        assertThat(result)
            .isNotNull()
            .isEqualTo("");
    }

    @Test
    void checkUserPermissionTest3() {
        final List<Permission> permissionsList = List.of(Permission.READ);
        final DocumentPermissions doc = DocumentPermissions.builder()
            .id(DOCUMENT_ID)
            .permissions(permissionsList)
            .build();
        when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(Optional.of(doc));

        final Document document = buildDocument("1234567890123456", "", "");

        assertThrows(ForbiddenException.class, () -> sut.checkUserPermission(document.getCaseId(),
                                                                             DOCUMENT_ID,
                                                                             Permission.CREATE,
                                                                             USER_PERMISSION_ERROR,
                                                                             "exception string"));
    }

    private Document buildDocument(final String caseId, final String caseTypeId, final String jurisdictionId) {
        final Map<String, String> myMap = Map.of(
            METADATA_CASE_ID, caseId,
            METADATA_CASE_TYPE_ID, caseTypeId,
            METADATA_JURISDICTION_ID, jurisdictionId
        );

        return Document.builder()
            .metadata(myMap)
            .build();
    }

    private void mockAuthorisedServices() {
        final AuthorisedService authorisedService = AuthorisedService.builder()
            .id(SERVICE_NAME_XUI_WEBAPP)
            .caseTypeId(List.of("BEFTA_CASETYPE_1_1", "BEFTA_CASETYPE_2_1"))
            .build();

        doReturn(List.of(authorisedService)).when(authorisedServicesMock).getAuthServices();
    }

}
