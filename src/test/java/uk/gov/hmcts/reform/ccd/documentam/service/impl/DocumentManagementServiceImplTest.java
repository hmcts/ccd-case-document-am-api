package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DocumentStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.configuration.AuthorisedServicesConfiguration;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.documentam.util.ApplicationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PATCH;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.buildUpdateDocumentCommand;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.ORIGINAL_FILE_NAME;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_PERMISSION_ERROR;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.USER_PERMISSION_ERROR;

@ExtendWith(MockitoExtension.class)
class DocumentManagementServiceImplTest implements TestFixture {

    private static final String ORIGINAL_DOCUMENT_NAME = "filename.txt";
    private static final String MIME_TYPE = "application/octet-stream";
    private static final String DOCUMENT_ID_FROM_LINK = "80e9471e-0f67-42ef-8739-170aa1942363";
    private static final String SELF_LINK = "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363";
    private static final String BINARY_LINK = "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363/binary";

    private static final String BULK_SCAN_PROCESSOR = "bulk_scan_processor";

    @Mock
    private RestTemplate restTemplateMock;

    @Mock
    private DocumentStoreClient documentStoreClient;

    @Mock
    private CaseDataStoreService caseDataStoreServiceMock;

    private static final HttpEntity<Object> NULL_REQUEST_ENTITY = null;

    private DocumentManagementServiceImpl sut;

    private final String documentURL = "http://localhost:4506";
    private final String salt = "AAAOA7A2AA6AAAA5";
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

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        final AuthorisedServices authorisedServices = loadAuthorisedServices();

        sut = new DocumentManagementServiceImpl(restTemplateMock,
                                                documentStoreClient,
                                                caseDataStoreServiceMock,
                                                authorisedServices);

        ReflectionTestUtils.setField(sut, "documentTtlInDays", 1);
        ReflectionTestUtils.setField(sut, "documentURL", "http://localhost:4506");
        ReflectionTestUtils.setField(sut, "salt", "AAAOA7A2AA6AAAA5");
        ReflectionTestUtils.setField(sut, "bulkScanExceptionRecordTypes", bulkScanExceptionRecordTypes);
    }

    @Test
    void getDocumentMetadata_HappyPath() {
        // GIVEN
        final Document expectedDocument = Document.builder().build();
        stubDmStoreClient(expectedDocument);

        // WHEN
        final Document actualDocument = sut.getDocumentMetadata(DOCUMENT_ID);

        // THEN
        assertThat(actualDocument)
            .isNotNull()
            .isEqualTo(expectedDocument);

        verify(documentStoreClient).getDocument(DOCUMENT_ID);
    }

    @Test
    void getDocumentMetadata_OptionalEmpty() {
        doReturn(Optional.empty()).when(documentStoreClient).getDocument(DOCUMENT_ID);

        assertThatExceptionOfType(ResourceNotFoundException.class)
            .isThrownBy(() -> sut.getDocumentMetadata(DOCUMENT_ID))
            .withMessage("Meta data does not exist for documentId: " + DOCUMENT_ID);

        verify(documentStoreClient).getDocument(DOCUMENT_ID);
    }

    @Test
    void getDocumentBinaryContent_HappyPath() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ORIGINAL_FILE_NAME, "name");
        headers.add(CONTENT_DISPOSITION, "disp");
        headers.add(DATA_SOURCE, "source");
        headers.add(CONTENT_TYPE, "type");

        doReturn(new ResponseEntity<>(headers, HttpStatus.OK)).when(restTemplateMock).exchange(
            documentURL + "/documents/" + DOCUMENT_ID + "/binary",
            HttpMethod.GET,
            NULL_REQUEST_ENTITY,
            ByteArrayResource.class
        );

        final ResponseEntity<ByteArrayResource> responseEntity = sut.getDocumentBinaryContent(DOCUMENT_ID);

        assertThat(responseEntity)
            .isNotNull()
            .satisfies(entity -> {
                assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(entity.getHeaders()).isEqualTo(headers);
            });

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Try_ResponseNotOK() {
        final ByteArrayResource byteArrayResource = mock(ByteArrayResource.class);
        doReturn(new ResponseEntity<>(byteArrayResource, HttpStatus.BAD_REQUEST)).when(restTemplateMock).exchange(
            documentURL + "/documents/" + DOCUMENT_ID + "/binary",
            HttpMethod.GET,
            NULL_REQUEST_ENTITY,
            ByteArrayResource.class
        );

        final ResponseEntity<ByteArrayResource> responseEntity = sut.getDocumentBinaryContent(DOCUMENT_ID);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        verifyRestExchangeByteArray();
    }

    @ParameterizedTest
    @MethodSource("provideHttpErrorForDocumentParameters")
    void getDocumentBinaryContent_Throws_Exception(final HttpStatus status,
                                                   final Class<Throwable> clazz,
                                                   final String msgPrefix) {
        doThrow(new HttpClientErrorException(status)).when(restTemplateMock).exchange(
            documentURL + "/documents/" + DOCUMENT_ID + "/binary",
            HttpMethod.GET,
            NULL_REQUEST_ENTITY,
            ByteArrayResource.class);

        assertThatExceptionOfType(clazz)
            .isThrownBy(() -> sut.getDocumentBinaryContent(DOCUMENT_ID))
            .withMessage(msgPrefix + DOCUMENT_ID);

        verifyRestExchangeByteArray();
    }

    @Test
    void checkServicePermission_WhenJurisdictionIdIsNull() {
        final String jurisdictionId = null;

        assertThrows(NullPointerException.class, () ->
            sut.checkServicePermission(
                CASE_TYPE_ID_VALUE,
                jurisdictionId,
                XUI_WEBAPP,
                Permission.READ,
                "log string",
                "exception string"));
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

    private void stubDmStoreClient(final Document document) {
        doReturn(Optional.ofNullable(document)).when(documentStoreClient).getDocument(DOCUMENT_ID);
    }

    private void verifyRestExchangeByteArray() {
        verify(restTemplateMock, times(1))
            .exchange(documentURL + "/documents/" + DOCUMENT_ID + "/binary", HttpMethod.GET,
                      NULL_REQUEST_ENTITY, ByteArrayResource.class);
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
                salt.concat(DOCUMENT_ID.toString())
                    .concat(JURISDICTION_ID_VALUE)
                    .concat(CASE_TYPE_ID_VALUE)))
            .build();
        final List<DocumentHashToken> documentList = List.of(doc);

        final Map<String, String> myMetadata = Map.of(
            JURISDICTION_ID, JURISDICTION_ID_VALUE,
            CASE_TYPE_ID, CASE_TYPE_ID_VALUE
        );
        final Document document = Document.builder()
            .metadata(myMetadata)
            .build();

        stubDmStoreClient(document);

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID_VALUE)
                                                                           .caseTypeId(CASE_TYPE_ID_VALUE)
                                                                           .jurisdictionId(JURISDICTION_ID_VALUE)
                                                                           .documentHashTokens(documentList)
                                                                           .build();


        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        // WHEN
        sut.patchDocumentMetadata(caseDocumentsMetadata);

        // THEN
        verify(restTemplateMock).exchange(eq(documentURL + "/documents"),
                                          eq(PATCH),
                                          entityCaptor.capture(),
                                          eq(Void.class));

        final UpdateDocumentsCommand documentsCommand = (UpdateDocumentsCommand)entityCaptor.getValue().getBody();
        assertThat(documentsCommand.getTtl())
            .isNull();
    }

    @Test
    void shouldPatchMetaDataEvenIfTokenIsNotPassed_hashCheckDisabled() {
        ReflectionTestUtils.setField(sut, "hashCheckEnabled", false);

        final Document document = Document.builder()
            .metadata(Map.of(JURISDICTION_ID, JURISDICTION_ID_VALUE, CASE_TYPE_ID, "CMC_ExceptionRecord"))
            .build();

        stubDmStoreClient(document);

        final DocumentHashToken doc = DocumentHashToken.builder().id(DOCUMENT_ID).build();
        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        sut.patchDocumentMetadata(caseDocumentsMetadata);

        verify(restTemplateMock).exchange(
            eq(documentURL + "/documents"),
            eq(PATCH),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    void patchDocumentMetadata_Throws_NotFoundException() {
        final DocumentHashToken doc = DocumentHashToken.builder()
            .id(DOCUMENT_ID)
            .hashToken(ApplicationUtils.generateHashCode(
                salt.concat(DOCUMENT_ID.toString())
                    .concat(JURISDICTION_ID_VALUE)
                    .concat(CASE_TYPE_ID_VALUE)))
            .build();

        doReturn(Optional.empty()).when(documentStoreClient).getDocument(DOCUMENT_ID);

        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        assertThrows(ResourceNotFoundException.class, () -> sut.patchDocumentMetadata(caseDocumentsMetadata));
    }

    @Test
    // (2) are these the same?
    void shouldNotPatchMetaDataWhenDocumentNotMovingCase_noExceptionRecordType() {
        ReflectionTestUtils.setField(sut, "hashCheckEnabled", false);

        final Document document = Document.builder()
            .metadata(Map.of(JURISDICTION_ID, JURISDICTION_ID_VALUE, CASE_TYPE_ID, CASE_TYPE_ID_VALUE))
            .build();

        stubDmStoreClient(document);

        final DocumentHashToken doc = DocumentHashToken.builder().id(DOCUMENT_ID).build();
        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        assertThrows(BadRequestException.class, () -> sut.patchDocumentMetadata(caseDocumentsMetadata));
    }

    @Test
    // (2) are these the same? is this test still valid?
    @Disabled
    void patchDocumentMetadata_Throws_BadRequestException() {
        final String token = ApplicationUtils.generateHashCode(
            salt.concat(DOCUMENT_ID.toString())
                .concat(JURISDICTION_ID_VALUE)
                .concat(CASE_TYPE_ID_VALUE));
        final DocumentHashToken doc = DocumentHashToken.builder()
            .id(DOCUMENT_ID)
            .hashToken(token)
            .build();

        final Document document = Document.builder()
            .metadata(Map.of(JURISDICTION_ID, JURISDICTION_ID_VALUE, CASE_TYPE_ID, CASE_TYPE_ID_VALUE))
            .build();

        stubDmStoreClient(document);

        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .documentHashTokens(List.of(doc))
            .build();

        assertThrows(BadRequestException.class, () -> sut.patchDocumentMetadata(caseDocumentsMetadata));
    }

    @Test
    void shouldThrowForbiddenWhenTokenIsNotPassed_hashCheckEnabled() {
        ReflectionTestUtils.setField(sut, "hashCheckEnabled", true);

        final DocumentHashToken doc = DocumentHashToken.builder().id(DOCUMENT_ID).build();

        final Document document = Document.builder()
            .metadata(Map.of(JURISDICTION_ID, JURISDICTION_ID_VALUE, CASE_TYPE_ID, CASE_TYPE_ID_VALUE))
            .build();

        stubDmStoreClient(document);

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
    }

    @Test
    // (1) are these the same?
    void shouldThrowForbiddenWhenTokenIsNotMatched() {
        final String token = ApplicationUtils.generateHashCode(
            salt.concat(DOCUMENT_ID.toString())
                .concat(JURISDICTION_ID_VALUE)
                .concat(CASE_TYPE_ID_VALUE));
        final DocumentHashToken doc = DocumentHashToken.builder()
            .id(DOCUMENT_ID)
            .hashToken(token)
            .build();

        final Document document = Document.builder()
            .metadata(Map.of(JURISDICTION_ID, JURISDICTION_ID_VALUE, CASE_TYPE_ID, "DIFFERENT_CASETYPE"))
            .build();

        stubDmStoreClient(document);

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
    }

    @Test
    // (1) are these the same?
    void patchDocumentMetadata_Throws_ForbiddenException_InvalidHashToken() {
        final String invalidToken = ApplicationUtils.generateHashCode(
            salt.concat(DOCUMENT_ID.toString())
                .concat(JURISDICTION_ID_VALUE)
                .concat(CASE_TYPE_ID_VALUE));
        final DocumentHashToken doc = DocumentHashToken.builder()
            .id(DOCUMENT_ID)
            .hashToken(invalidToken)
            .build();

        final Map<String, String> myMetadata = Map.of(
            CASE_ID, CASE_ID_VALUE,
            JURISDICTION_ID, JURISDICTION_ID_VALUE,
            CASE_TYPE_ID, CASE_TYPE_ID_VALUE
        );
        final Document document = Document.builder()
            .metadata(myMetadata)
            .build();

        stubDmStoreClient(document);

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
    }

    @Test
    void patchDocumentMetadata_Throws_ServiceException() {
        final DocumentHashToken doc = DocumentHashToken.builder()
            .id(DOCUMENT_ID)
            .hashToken(ApplicationUtils.generateHashCode(
                salt.concat(DOCUMENT_ID.toString())
                    .concat(JURISDICTION_ID_VALUE)
                    .concat(CASE_TYPE_ID_VALUE)))
            .build();

        final Document document = Document.builder()
            .metadata(Map.of(JURISDICTION_ID, JURISDICTION_ID_VALUE, CASE_TYPE_ID, CASE_TYPE_ID_VALUE))
            .build();

        stubDmStoreClient(document);

        doThrow(new HttpClientErrorException(HttpStatus.BAD_GATEWAY))
            .when(restTemplateMock).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Void.class));

        final CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID_VALUE)
                                                                           .caseTypeId(CASE_TYPE_ID_VALUE)
                                                                           .jurisdictionId(JURISDICTION_ID_VALUE)
                                                                           .documentHashTokens(List.of(doc))
                                                                           .build();

        assertThatExceptionOfType(ServiceException.class)
            .isThrownBy(() -> sut.patchDocumentMetadata(caseDocumentsMetadata))
            .withMessage("Exception occurred with operation");
    }

    @Test
    void uploadDocuments_HappyPath() {

        Document.Links links = getLinks();

        Document document = Document.builder()
            .size(1000L)
            .mimeType(MIME_TYPE)
            .originalDocumentName(ORIGINAL_DOCUMENT_NAME)
            .classification(Classification.PUBLIC)
            .links(links)
            .build();

        DmUploadResponse dmUploadResponse = DmUploadResponse.builder()
            .embedded(DmUploadResponse.Embedded.builder().documents(List.of(document)).build())
            .build();

        Mockito.when(restTemplateMock.postForObject(anyString(), any(HttpEntity.class), eq(DmUploadResponse.class)))
            .thenReturn(dmUploadResponse);

        String expectedHash = ApplicationUtils
            .generateHashCode(salt.concat(DOCUMENT_ID_FROM_LINK
                                              .concat(JURISDICTION_ID_VALUE)
                                              .concat(CASE_TYPE_ID_VALUE)));

        UploadResponse response = sut.uploadDocuments(
            List.of(new MockMultipartFile("afile", "some".getBytes())),
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
            JURISDICTION_ID_VALUE
        );

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
            }
            );

    }

    @ParameterizedTest
    @MethodSource("provideDocumentUploadParameters")
    void uploadDocuments_Throw_Exception(final HttpStatus status, final Class<Throwable> clazz, final String errorMsg) {
        doThrow(new HttpClientErrorException(status)).when(restTemplateMock)
            .postForObject(anyString(), any(HttpEntity.class), eq(DmUploadResponse.class));

        assertThatExceptionOfType(clazz)
            .isThrownBy(() -> sut.uploadDocuments(
                emptyList(),
                "classification",
                CASE_TYPE_ID_VALUE,
                JURISDICTION_ID_VALUE
            ))
            .withMessage(errorMsg);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideDocumentUploadParameters() {
        return Stream.of(
            Arguments.of(HttpStatus.BAD_GATEWAY, ServiceException.class, "Exception occurred with operation"),
            Arguments.of(HttpStatus.FORBIDDEN, ForbiddenException.class, "Forbidden: Insufficient permissions"),
            Arguments.of(HttpStatus.BAD_REQUEST, ServiceException.class, "Exception occurred with operation"),
            Arguments.of(HttpStatus.NOT_FOUND, ResourceNotFoundException.class, "Resource not found")
        );
    }

    @Test
    void patchDocument_HappyPath() {
        final UpdateTtlRequest ttlRequest = buildUpdateDocumentCommand();
        final HttpEntity<DmTtlRequest> requestEntity = getTtlRequestEntity(ttlRequest);
        final String patchTTLUrl = String.format("%s/documents/%s", documentURL, DOCUMENT_ID);

        final ResponseEntity<PatchDocumentResponse> expectedResponseEntity = new ResponseEntity<>(
            PatchDocumentResponse.builder().build(),
            HttpStatus.OK
        );
        doReturn(expectedResponseEntity)
            .when(restTemplateMock).exchange(patchTTLUrl, PATCH, requestEntity, PatchDocumentResponse.class);

        final ResponseEntity<PatchDocumentResponse> responseEntity = sut.patchDocument(DOCUMENT_ID, ttlRequest);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @ParameterizedTest
    @MethodSource("provideHttpErrorForDocumentParameters")
    void patchDocument_Throw_Exception(final HttpStatus status, final Class<Throwable> clazz, final String msgPrefix) {
        final UpdateTtlRequest ttlRequest = buildUpdateDocumentCommand();
        final HttpEntity<DmTtlRequest> requestEntity = getTtlRequestEntity(ttlRequest);

        doThrow(new HttpClientErrorException(status)).when(restTemplateMock)
            .exchange(String.format("%s/documents/%s", documentURL, DOCUMENT_ID),
                      PATCH,
                      requestEntity,
                      PatchDocumentResponse.class);

        assertThatExceptionOfType(clazz)
            .isThrownBy(() -> sut.patchDocument(DOCUMENT_ID, ttlRequest))
            .withMessage(msgPrefix + DOCUMENT_ID);
    }

    private HttpEntity<DmTtlRequest> getTtlRequestEntity(UpdateTtlRequest ttlRequest) {
        return new HttpEntity<>(new DmTtlRequest(ttlRequest.getTtl().atZone(
            ZoneId.systemDefault())));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void deleteDocument_HappyPath() {
        Boolean permanent = true;

        sut.deleteDocument(DOCUMENT_ID, permanent);

        verify(restTemplateMock).exchange(
            String.format("%s/documents/%s?permanent=%s", documentURL, DOCUMENT_ID, permanent),
            DELETE,
            NULL_REQUEST_ENTITY,
            Void.class
        );
    }

    @ParameterizedTest
    @MethodSource("provideHttpErrorForDocumentParameters")
    @SuppressWarnings("ConstantConditions")
    void deleteDocument_Throw_Exception(final HttpStatus status, final Class<Throwable> clazz, final String msgPrefix) {
        final Boolean permanent = true;

        doThrow(new HttpClientErrorException(status)).when(restTemplateMock)
            .exchange(String.format("%s/documents/%s?permanent=" + permanent, documentURL, DOCUMENT_ID),
                      DELETE,
                      NULL_REQUEST_ENTITY,
                      Void.class);

        assertThatExceptionOfType(clazz)
            .isThrownBy(() -> sut.deleteDocument(DOCUMENT_ID, permanent))
            .withMessage(msgPrefix + DOCUMENT_ID);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideHttpErrorForDocumentParameters() {
        final String serviceExceptionMessagePrefix = "Exception occurred with operation on document id: ";

        return Stream.of(
            Arguments.of(HttpStatus.BAD_GATEWAY, ServiceException.class, serviceExceptionMessagePrefix),
            Arguments.of(HttpStatus.FORBIDDEN, ForbiddenException.class, "Forbidden: Insufficient permissions: "),
            Arguments.of(HttpStatus.BAD_REQUEST, ServiceException.class, serviceExceptionMessagePrefix),
            Arguments.of(HttpStatus.NOT_FOUND, ResourceNotFoundException.class, "Resource not found ")
        );
    }

    @Test
    void shouldGenerateHashTokenWhenCaseIdIsPresent() {
        final Document document = buildDocument(CASE_ID_VALUE, "BEFTA_CASETYPE_2_2", JURISDICTION_ID_VALUE);

        stubDmStoreClient(document);

        final String result = sut.generateHashToken(DOCUMENT_ID);

        assertNotNull(result);
    }

    @Test
    void shouldGenerateHashTokenWhenCaseIdIsNotPresent() {
        final Document document = Document.builder()
            .metadata(Map.of(CASE_TYPE_ID, "BEFTA_CASETYPE_2_2", JURISDICTION_ID, JURISDICTION_ID_VALUE))
            .build();

        stubDmStoreClient(document);

        final String result = sut.generateHashToken(DOCUMENT_ID);

        assertNotNull(result);
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
            CASE_ID, caseId,
            CASE_TYPE_ID, caseTypeId,
            JURISDICTION_ID, jurisdictionId
        );

        return Document.builder()
            .metadata(myMap)
            .build();
    }

    private Document.Links getLinks() {
        Document.Links links = new Document.Links();

        Document.Link self = new Document.Link();
        Document.Link binary = new Document.Link();
        self.href = SELF_LINK;
        binary.href = BINARY_LINK;

        links.self = self;
        links.binary = binary;
        return links;
    }

    private AuthorisedServices loadAuthorisedServices() throws IOException {
        try (final InputStream inputStream = AuthorisedServicesConfiguration.class.getClassLoader()
            .getResourceAsStream("service_config.json")) {

            return TestFixture.objectMapper().readValue(inputStream, AuthorisedServices.class);
        }
    }

}
