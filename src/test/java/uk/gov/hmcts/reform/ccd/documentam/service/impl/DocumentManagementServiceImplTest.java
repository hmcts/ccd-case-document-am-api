package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
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
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.documentam.util.ApplicationUtils;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PATCH;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.buildUpdateDocumentCommand;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_LENGTH;
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

    private static final String USER_ID = "d5566a63-f87c-4658-a4d6-213d949f8415";

    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";
    private static final UUID MATCHED_DOCUMENT_UUID = UUID.fromString(MATCHED_DOCUMENT_ID);
    private static final String CASE_ID = "1582550122096256";
    private static final String BEFTA_CASETYPE_2 = "BEFTA_CASETYPE_2";
    private static final String BEFTA_EXCEPTION_CASETYPE_2 = "CMC_ExceptionRecord";
    private static final String BEFTA_JURISDICTION_2 = "BEFTA_JURISDICTION_2";
    private static final String XUI_WEBAPP = "xui_webapp";
    private static final String BULK_SCAN_PROCESSOR = "bulk_scan_processor";
    private final RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);
    private final CaseDataStoreService caseDataStoreServiceMock = mock(CaseDataStoreService.class);
    private AuthorisedServices authorisedServicesMock = mock(AuthorisedServices.class);

    private static final HttpEntity<Object> NULL_REQUEST_ENTITY = null;

    @InjectMocks
    private final DocumentManagementServiceImpl sut = new DocumentManagementServiceImpl(restTemplateMock,
                                                                                        caseDataStoreServiceMock,
                                                                                        authorisedServicesMock
    );

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
    void setUp() {
        ReflectionTestUtils.setField(sut, "documentTtlInDays", 1);
        ReflectionTestUtils.setField(sut, "documentURL", "http://localhost:4506");
        ReflectionTestUtils.setField(sut, "salt", "AAAOA7A2AA6AAAA5");
        ReflectionTestUtils.setField(sut, "bulkScanExceptionRecordTypes", bulkScanExceptionRecordTypes);
    }

    @Test
    void getDocumentMetadata_HappyPath() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        Optional documentMetadata = sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID);
        assertTrue(documentMetadata.isPresent());

        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_ServiceException() {
        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, NULL_REQUEST_ENTITY,
            StoredDocumentHalResource.class))
               .thenThrow(HttpClientErrorException.create("woopsie", HttpStatus.BAD_GATEWAY, "404",
                                                          new HttpHeaders(),
                                                          new byte[1],
                                                          Charset.defaultCharset()));

        assertThrows(ServiceException.class, () -> {
            sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID);
        });

        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_ResourceNotFoundException() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.BAD_REQUEST);
        assertThrows(ResourceNotFoundException.class, () -> {
            sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_ResourceNotFoundException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.NOT_FOUND,
                                                                                            "woopsie",
                                                                                            new HttpHeaders(),
                                                                                            null,
                                                                                            null);
        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, NULL_REQUEST_ENTITY,
            StoredDocumentHalResource.class))
               .thenThrow(httpClientErrorException);

        Optional<StoredDocumentHalResource> documentMetadata = sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID);
        assertTrue(documentMetadata.isEmpty());

        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_ForbiddenException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.FORBIDDEN,
                                                                                            "woopsie",
                                                                                            new HttpHeaders(),
                                                                                            null,
                                                                                            null);
        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, NULL_REQUEST_ENTITY,
            StoredDocumentHalResource.class))
               .thenThrow(httpClientErrorException);
        assertThrows(ForbiddenException.class, () -> sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID));
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_BadRequestException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.BAD_REQUEST,
                                                                                            "woopsie",
                                                                                            new HttpHeaders(),
                                                                                            null,
                                                                                            null);
        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, NULL_REQUEST_ENTITY,
            StoredDocumentHalResource.class))
               .thenThrow(httpClientErrorException);
        assertThrows(BadRequestException.class, () -> {
            sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_ServiceException() {
        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, NULL_REQUEST_ENTITY,
            StoredDocumentHalResource.class))
               .thenThrow(HttpClientErrorException.class);

        assertThrows(ServiceException.class, () -> sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID));
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void extractCaseIdFromMetadata_HappyPath() {
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("1234qwer1234qwer", "", ""),
                                         HttpStatus.OK);
        Optional<StoredDocumentHalResource> documentMetadata = sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID);
        assertTrue(documentMetadata.isPresent());


        String caseId = sut.extractCaseIdFromMetadata(documentMetadata.get());
        assertEquals("1234qwer1234qwer", caseId);
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void extractCaseIdFromMetadata_InvalidObjectType() {
        String response = sut.extractCaseIdFromMetadata(null);
        assertNull(response);
    }

    @Test
    void getDocumentBinaryContent_HappyPath() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ORIGINAL_FILE_NAME, "name");
        headers.add(CONTENT_DISPOSITION, "disp");
        headers.add(DATA_SOURCE, "source");
        headers.add(CONTENT_TYPE, "type");

        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET,
            NULL_REQUEST_ENTITY,
            ByteArrayResource.class
                                              )).thenReturn(new ResponseEntity<ByteArrayResource>(headers,
                                                                                                  HttpStatus.OK));
        ResponseEntity responseEntity = sut.getDocumentBinaryContent(MATCHED_DOCUMENT_UUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        assertEquals(responseEntity.getHeaders(), headers);

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_ServiceException() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ORIGINAL_FILE_NAME, "name");
        headers.add(CONTENT_DISPOSITION, "disp");
        headers.add(DATA_SOURCE, "source");
        headers.add(CONTENT_TYPE, "type");
        headers.add(CONTENT_LENGTH, "length");

        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET,
            NULL_REQUEST_ENTITY,
            ByteArrayResource.class
                                              )).thenThrow(HttpClientErrorException.create("woopsie",
                                                                                           HttpStatus.BAD_GATEWAY,
                                                                                           "404",
                                                                                           new HttpHeaders(),
                                                                                           new byte[1],
                                                                                           Charset.defaultCharset()));

        assertThrows(ServiceException.class, () -> sut.getDocumentBinaryContent(MATCHED_DOCUMENT_UUID));

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Try_ResponseNotOK() {
        ByteArrayResource byteArrayResource = mock(ByteArrayResource.class);
        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET, NULL_REQUEST_ENTITY, ByteArrayResource.class))
               .thenReturn(new ResponseEntity<>(byteArrayResource, HttpStatus.BAD_REQUEST));
        ResponseEntity responseEntity = sut.getDocumentBinaryContent(MATCHED_DOCUMENT_UUID);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Throws_HttpClientErrorException_ResourceNotFoundException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.NOT_FOUND,
                                                                                            "woopsie",
                                                                                            new HttpHeaders(),
                                                                                            null,
                                                                                            null);
        mockitoWhenRestExchangeByteArrayThenThrow(httpClientErrorException);

        assertThrows(ResourceNotFoundException.class, () -> sut.getDocumentBinaryContent(MATCHED_DOCUMENT_UUID));

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Throws_HttpClientErrorException_ForbiddenException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.FORBIDDEN,
                                                                                            "woopsie",
                                                                                            new HttpHeaders(),
                                                                                            null,
                                                                                            null);
        mockitoWhenRestExchangeByteArrayThenThrow(httpClientErrorException);

        assertThrows(ForbiddenException.class, () -> sut.getDocumentBinaryContent(MATCHED_DOCUMENT_UUID));

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Throws_HttpClientErrorException_BadRequestException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.BAD_REQUEST,
                                                                                            "woopsie",
                                                                                            new HttpHeaders(),
                                                                                            null,
                                                                                            null);
        mockitoWhenRestExchangeByteArrayThenThrow(httpClientErrorException);

        assertThrows(BadRequestException.class, () -> sut.getDocumentBinaryContent(MATCHED_DOCUMENT_UUID));

        verifyRestExchangeByteArray();
    }

    private void mockitoWhenRestExchangeByteArrayThenThrow(HttpClientErrorException httpClientErrorException) {
        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET, NULL_REQUEST_ENTITY,
            ByteArrayResource.class))
               .thenThrow(httpClientErrorException);
    }

    @Test
    void getDocumentBinaryContent_Throws_HttpClientErrorException_ServiceException() {
        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET, NULL_REQUEST_ENTITY,
            ByteArrayResource.class))
               .thenThrow(HttpClientErrorException.class);

        assertThrows(ServiceException.class, () -> {
            sut.getDocumentBinaryContent(MATCHED_DOCUMENT_UUID);
        });

        verifyRestExchangeByteArray();
    }

    @Test
    void checkServicePermission_WhenJurisdictionIdIsNull() {
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("", "caseTypeId",
                                                               ""), HttpStatus.OK);
        mockAuthorisedServices();

        assertThrows(NullPointerException.class, () ->
            sut.checkServicePermission(new StoredDocumentHalResource(),
                                       XUI_WEBAPP,
                                       Permission.READ,
                                       "log string",
                                       "exception string"));
    }

    @Test
    void checkServicePermission_CaseTypeIdExistsButNotAuthorised() {
        assertThrows(ForbiddenException.class, () ->
            sut.checkServicePermission(initialiseMetaDataMap("caseId", "randomCaseTypeId",
                                                             "juridiction"),
                                       XUI_WEBAPP,
                                       Permission.READ,
                                       "log string",
                                       "exception string"));
    }

    @Test
    void checkServicePermissionForUpload_WhenServiceIsNotAuthorised() {
        assertThrows(ForbiddenException.class, () -> sut.checkServicePermission(
            "caseTypeId",
            "BEFTA_JURISDICTION_2",
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
            "BEFTA_JURISDICTION_2",
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
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("1234567812345678", "caseTypeId",
                                                               "BEFTA_JURISDICTION_2"), HttpStatus.OK);
        assertThrows(ForbiddenException.class, () -> sut.checkServicePermission(
            new StoredDocumentHalResource(),
            "bad_Service_name",
            Permission.READ,
            "log string",
            "exception string"));
    }

    @Test
    void checkUserPermission_Throws_CaseNotFoundException() {
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("1234567890123456",
                                                               "", ""), HttpStatus.OK);
        Optional<StoredDocumentHalResource> documentMetadata = sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID);
        assertTrue(documentMetadata.isPresent());

        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(null);

        assertThrows(Exception.class, () -> {
            sut.checkUserPermission(documentMetadata.get(),
                                    MATCHED_DOCUMENT_UUID,
                                    Permission.READ,
                                    USER_PERMISSION_ERROR,
                                    "exception string");
        });

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    @Test
    void checkUserPermission_ReturnsFalse_Scenario1() {
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("1234567890123456",
                                                               "", ""), HttpStatus.OK);
        Optional<StoredDocumentHalResource> documentMetadata = sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID);
        assertTrue(documentMetadata.isPresent());

        List<Permission> permissionsList = new ArrayList<>();
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_UUID).permissions(permissionsList).build();
        when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(Optional.ofNullable(doc));

        assertThrows(ForbiddenException.class, () -> sut.checkUserPermission(documentMetadata.get(),
                                                                             MATCHED_DOCUMENT_UUID,
                                                                             Permission.READ,
                                                                             USER_PERMISSION_ERROR,
                                                                             "exception string"));

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }


    @Test
    void checkUserPermission_ReturnsFalse_Scenario2() {
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("1234567890123456",
                                                               "", ""), HttpStatus.OK);
        Optional<StoredDocumentHalResource> documentMetadata = sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID);
        assertTrue(documentMetadata.isPresent());

        List<Permission> permissionsList = new ArrayList<>();
        DocumentPermissions doc;
        doc =
            DocumentPermissions.builder().id(UUID.fromString("40000a2b-00ce-00eb-0068-2d00a700be9c"))
                .permissions(permissionsList)
                               .build();
        when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(Optional.ofNullable(doc));

        assertThrows(ForbiddenException.class, () -> sut.checkUserPermission(documentMetadata.get(),
                                                                             MATCHED_DOCUMENT_UUID,
                                                                             Permission.READ,
                                                                             USER_PERMISSION_ERROR,
                                                                             "exception string"));

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    private void mockitoWhenRestExchangeThenThrow(StoredDocumentHalResource storedDocumentHalResource,
                                                  HttpStatus httpStatus) {
        when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, NULL_REQUEST_ENTITY,
            StoredDocumentHalResource.class))
               .thenReturn(new ResponseEntity<>(storedDocumentHalResource, httpStatus));
    }

    private void verifyRestExchangeOnStoredDoc() {
        verify(restTemplateMock, times(1))
            .exchange(documentURL + "/documents/" + MATCHED_DOCUMENT_ID, HttpMethod.GET, NULL_REQUEST_ENTITY,
                      StoredDocumentHalResource.class);
    }

    private void verifyRestExchangeByteArray() {
        verify(restTemplateMock, times(1))
            .exchange(documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary", HttpMethod.GET,
                      NULL_REQUEST_ENTITY, ByteArrayResource.class);
    }

    private void verifyCaseDataServiceGetDocMetadata() {
        verify(caseDataStoreServiceMock, times(1))
            .getCaseDocumentMetadata(anyString(), any(UUID.class));
    }

    @Test
    void patchDocumentMetadata_HappyPath() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_UUID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID.toString())
                                                         .concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        myMetadata.put("caseTypeId", BEFTA_CASETYPE_2);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID_VALUE)
                                                                           .caseTypeId(BEFTA_CASETYPE_2)
                                                                           .jurisdictionId(BEFTA_JURISDICTION_2)
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
    void shouldThrowForbiddenWhenTokenIsNotMatched() {

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        // to generate different hashToken
        myMetadata.put("caseTypeId", "DIFFERENT_CASETYPE");
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_UUID)
            .hashToken(ApplicationUtils.generateHashCode(
                salt.concat(MATCHED_DOCUMENT_ID.toString()).concat(BEFTA_JURISDICTION_2)
                    .concat(BEFTA_CASETYPE_2))).build();

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(BEFTA_CASETYPE_2)
            .jurisdictionId(BEFTA_JURISDICTION_2)
            .documentHashTokens(List.of(doc))
            .build();

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> sut.patchDocumentMetadata(caseDocumentsMetadata))
            .withMessage("Forbidden: Insufficient permissions: Hash token check failed for the document: "
                             + MATCHED_DOCUMENT_ID);
    }

    @Test
    void shouldThrowForbiddenWhenTokenIsNotPassed_hashCheckEnabled() {

        ReflectionTestUtils.setField(sut, "hashCheckEnabled", true);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        myMetadata.put("caseTypeId", BEFTA_CASETYPE_2);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(BEFTA_CASETYPE_2)
            .jurisdictionId(BEFTA_JURISDICTION_2)
            .documentHashTokens(List.of(DocumentHashToken.builder().id(MATCHED_DOCUMENT_UUID).build()))
            .build();

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> sut.patchDocumentMetadata(caseDocumentsMetadata))
            .withMessage(String.format(
                Constants.FORBIDDEN + ": %s",
                "Hash check is enabled but hashToken wasn't provided for the document:"
                    + MATCHED_DOCUMENT_ID
            ));
    }

    @Test
    void shouldPatchMetaDataEvenIfTokenIsNotPassed_hashCheckDisabled() {

        ReflectionTestUtils.setField(sut, "hashCheckEnabled", false);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        myMetadata.put("caseTypeId", BEFTA_EXCEPTION_CASETYPE_2);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .caseTypeId(BEFTA_CASETYPE_2)
            .jurisdictionId(BEFTA_JURISDICTION_2)
            .documentHashTokens(List.of(DocumentHashToken.builder().id(MATCHED_DOCUMENT_UUID).build()))
            .build();

        sut.patchDocumentMetadata(caseDocumentsMetadata);

        verify(restTemplateMock, times(1))
            .exchange(eq(documentURL + "/documents"), eq(PATCH), any(HttpEntity.class),
                      eq(Void.class));
    }

    @Test
    void shouldNotPatchMetaDataWhenDocumentNotMovingCase_noExceptionRecordType() {

        ReflectionTestUtils.setField(sut, "hashCheckEnabled", false);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        myMetadata.put("caseTypeId", BEFTA_CASETYPE_2);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        Mockito.when(restTemplateMock.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(Void.class)))
            .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "woopsie",
                                                       new HttpHeaders(), null,
                                                       null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID)
            .caseTypeId(BEFTA_CASETYPE_2)
            .jurisdictionId(BEFTA_JURISDICTION_2)
            .documentHashTokens(List.of(DocumentHashToken.builder().id(MATCHED_DOCUMENT_UUID).build()))
            .build();

        assertThrows(BadRequestException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    void patchDocumentMetadata_Throws_NotFoundException() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_UUID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID.toString())
                                                         .concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        myMetadata.put("caseTypeId", BEFTA_CASETYPE_2);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        when(restTemplateMock.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(Void.class)))
               .thenThrow(
                   HttpClientErrorException.create(HttpStatus.NOT_FOUND, "woopsie",
                                                   new HttpHeaders(), null, null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID_VALUE)
                                                                           .caseTypeId(BEFTA_CASETYPE_2)
                                                                           .jurisdictionId(BEFTA_JURISDICTION_2)
                                                                           .documentHashTokens(documentList)
                                                                           .build();

        assertThrows(ResourceNotFoundException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    void patchDocumentMetadata_Throws_ForbiddenException() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_UUID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID.toString())
                                                         .concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        myMetadata.put("caseTypeId", BEFTA_CASETYPE_2);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        when(restTemplateMock.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(Void.class)))
               .thenThrow(
                   HttpClientErrorException.create(HttpStatus.FORBIDDEN, "woopsie",
                                                   new HttpHeaders(), null, null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID_VALUE)
                                                                           .caseTypeId(BEFTA_CASETYPE_2)
                                                                           .jurisdictionId(BEFTA_JURISDICTION_2)
                                                                           .documentHashTokens(documentList)
                                                                           .build();

        assertThrows(ForbiddenException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    void patchDocumentMetadata_Throws_BadRequestException() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_UUID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID.toString())
                                                         .concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        myMetadata.put("caseTypeId", BEFTA_CASETYPE_2);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        when(restTemplateMock.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(Void.class)))
               .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "woopsie",
                                                          new HttpHeaders(), null,
                                                          null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID_VALUE)
                                                                           .caseTypeId(BEFTA_CASETYPE_2)
                                                                           .jurisdictionId(BEFTA_JURISDICTION_2)
                                                                           .documentHashTokens(documentList)
                                                                           .build();

        assertThrows(BadRequestException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    void patchDocumentMetadata_Throws_ForbiddenException_InvalidHashToken() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_UUID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID.toString())
                                                         .concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("caseId", CASE_ID_VALUE);
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        myMetadata.put("caseTypeId", BEFTA_CASETYPE_2);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        when(restTemplateMock.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(Void.class)))
               .thenThrow(
                   HttpClientErrorException.create(HttpStatus.FORBIDDEN, "woopsie",
                                                   new HttpHeaders(), null, null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID_VALUE)
                                                                           .caseTypeId(BEFTA_CASETYPE_2)
                                                                           .jurisdictionId(BEFTA_JURISDICTION_2)
                                                                           .documentHashTokens(documentList)
                                                                           .build();

        assertThrows(ForbiddenException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    void patchDocumentMetadata_Throws_BadGatewayException() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_UUID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID.toString())
                                                         .concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        myMetadata.put("caseTypeId", BEFTA_CASETYPE_2);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        when(restTemplateMock.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(Void.class)))
               .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_GATEWAY,
                                                          "woopsie", new HttpHeaders(), null,
                                                          null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID_VALUE)
                                                                           .caseTypeId(BEFTA_CASETYPE_2)
                                                                           .jurisdictionId(BEFTA_JURISDICTION_2)
                                                                           .documentHashTokens(documentList)
                                                                           .build();

        assertThrows(ServiceException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
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
                                              .concat(BEFTA_JURISDICTION_2)
                                              .concat(BEFTA_CASETYPE_2)));

        UploadResponse response = sut.uploadDocuments(
            List.of(new MockMultipartFile("afile", "some".getBytes())),
            Classification.PUBLIC.name(),
            BEFTA_CASETYPE_2,
            BEFTA_JURISDICTION_2
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
    void uploadDocuments_Throw_ServiceException(final HttpStatus status,
                                                final Class<Throwable> clazz) {

        when(restTemplateMock.postForObject(anyString(), any(HttpEntity.class), eq(DmUploadResponse.class)))
            .thenThrow(HttpClientErrorException.create(status, "woopsie", new HttpHeaders(), null, null));

        assertThrows(clazz, () -> sut.uploadDocuments(emptyList(),
                                                      "classification",
                                                      BEFTA_CASETYPE_2,
                                                      BEFTA_JURISDICTION_2
        ));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideDocumentUploadParameters() {
        return Stream.of(
            Arguments.of(HttpStatus.BAD_GATEWAY, ServiceException.class),
            Arguments.of(HttpStatus.FORBIDDEN, ForbiddenException.class),
            Arguments.of(HttpStatus.BAD_REQUEST, BadRequestException.class),
            Arguments.of(HttpStatus.NOT_FOUND, ResourceNotFoundException.class)
        );
    }

    @Test
    void patchDocument_HappyPath() {
        final UpdateTtlRequest ttlRequest = buildUpdateDocumentCommand();
        final HttpEntity<DmTtlRequest> requestEntity = getTtlRequestEntity(ttlRequest);
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
                                      )).thenReturn(new ResponseEntity<>(storedDocumentHalResource, HttpStatus.OK));

        ResponseEntity responseEntity = sut.patchDocument(MATCHED_DOCUMENT_UUID, ttlRequest);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    void patchDocument_ResourceNotFound() {
        final UpdateTtlRequest ttlRequest = buildUpdateDocumentCommand();
        final HttpEntity<DmTtlRequest> requestEntity = getTtlRequestEntity(ttlRequest);
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
                                      )).thenReturn(new ResponseEntity<>(storedDocumentHalResource,
                                                                         HttpStatus.NOT_FOUND));

        assertThrows(ResourceNotFoundException.class, () -> {
            sut.patchDocument(MATCHED_DOCUMENT_UUID, ttlRequest);
        });
    }

    @Test
    void patchDocument_BadRequest() {
        final UpdateTtlRequest ttlRequest = buildUpdateDocumentCommand();
        final HttpEntity<DmTtlRequest> requestEntity = getTtlRequestEntity(ttlRequest);
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
                                      )).thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST,
                                                                                   "woopsie",
                                                                                   new HttpHeaders(),
                                                                                   null, null));

        assertThrows(BadRequestException.class, () -> {
            sut.patchDocument(MATCHED_DOCUMENT_UUID, ttlRequest);
        });
    }

    @Test
    void patchDocument_Forbidden() {
        final UpdateTtlRequest ttlRequest = buildUpdateDocumentCommand();
        final HttpEntity<DmTtlRequest> requestEntity = getTtlRequestEntity(ttlRequest);
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
                                      )).thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN,
                                                                                   "woopsie",
                                                                                   new HttpHeaders(),
                                                                                   null, null));

        assertThrows(ForbiddenException.class, () -> {
            sut.patchDocument(MATCHED_DOCUMENT_UUID, ttlRequest);
        });
    }

    @Test
    void patchDocument_HttpClientErrorException() {
        final UpdateTtlRequest ttlRequest = buildUpdateDocumentCommand();
        final HttpEntity<DmTtlRequest> requestEntity = getTtlRequestEntity(ttlRequest);
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
                                      )).thenThrow(HttpClientErrorException.NotFound.create("woopsie",
                                                                                            HttpStatus.NOT_FOUND,
                                                                                            "404",
                                                                                            new HttpHeaders(),
                                                                                            new byte[1],
                                                                                            Charset.defaultCharset()));

        assertThrows(ResourceNotFoundException.class, () ->
            sut.patchDocument(MATCHED_DOCUMENT_UUID, ttlRequest));
    }

    @Test
    void patchDocument_ServiceException() {
        final UpdateTtlRequest ttlRequest = buildUpdateDocumentCommand();
        final HttpEntity<DmTtlRequest> requestEntity = getTtlRequestEntity(ttlRequest);
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
                                      )).thenThrow(HttpClientErrorException.NotFound.create("woopsie",
                                                                                            HttpStatus.BAD_GATEWAY,
                                                                                            "403",
                                                                                            new HttpHeaders(),
                                                                                            new byte[1],
                                                                                            Charset.defaultCharset()));

        assertThrows(ServiceException.class, () -> {
            sut.patchDocument(MATCHED_DOCUMENT_UUID, ttlRequest);
        });
    }

    private HttpEntity<DmTtlRequest> getTtlRequestEntity(UpdateTtlRequest ttlRequest) {
        return new HttpEntity<>(new DmTtlRequest(ttlRequest.getTtl().atZone(
            ZoneId.systemDefault())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_HappyPath() {
        Boolean permanent = true;
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=%s", documentURL, MATCHED_DOCUMENT_ID,
                                                 permanent
        );

        sut.deleteDocument(MATCHED_DOCUMENT_UUID, permanent);

        verify(restTemplateMock).exchange(
            documentDeleteUrl,
            DELETE,
            NULL_REQUEST_ENTITY,
            Void.class
        );
    }


    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_NotFoundException() {
        Boolean permanent = true;

        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL,
                                                 MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            NULL_REQUEST_ENTITY,
            Void.class
                                      )).thenThrow(HttpClientErrorException.NotFound.create("woopsie",
                                                                                            HttpStatus.NOT_FOUND,
                                                                                            "404",
                                                                                            new HttpHeaders(),
                                                                                            new byte[1],
                                                                                            Charset.defaultCharset()));

        assertThrows(ResourceNotFoundException.class, () -> {
            sut.deleteDocument(MATCHED_DOCUMENT_UUID, permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_ForbiddenException() {
        Boolean permanent = true;

        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL,
                                                 MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            NULL_REQUEST_ENTITY,
            Void.class
                                      )).thenThrow(HttpClientErrorException.NotFound.create("woopsie",
                                                                                            HttpStatus.FORBIDDEN,
                                                                                            "404",
                                                                                            new HttpHeaders(),
                                                                                            new byte[1],
                                                                                            Charset.defaultCharset()));

        assertThrows(ForbiddenException.class, () -> {
            sut.deleteDocument(MATCHED_DOCUMENT_UUID, permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_BadRequestException() {
        Boolean permanent = true;

        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL,
                                                 MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            NULL_REQUEST_ENTITY,
            Void.class
                                      )).thenThrow(HttpClientErrorException.NotFound.create("woopsie",
                                                                                            HttpStatus.BAD_REQUEST,
                                                                                            "404",
                                                                                            new HttpHeaders(),
                                                                                            new byte[1],
                                                                                            Charset.defaultCharset()));

        assertThrows(BadRequestException.class, () -> {
            sut.deleteDocument(MATCHED_DOCUMENT_UUID, permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_ServiceException() {
        final Boolean permanent = true;

        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL,
                                                 MATCHED_DOCUMENT_ID);

        doThrow(
            HttpClientErrorException.NotFound
                .create(
                    "woopsie",
                    HttpStatus.BAD_GATEWAY,
                    "404",
                    new HttpHeaders(),
                    new byte[1],
                    Charset.defaultCharset()
                ))
            .when(restTemplateMock).exchange(
                documentDeleteUrl,
                DELETE,
                NULL_REQUEST_ENTITY,
                Void.class
            );

        assertThrows(ServiceException.class, () -> sut.deleteDocument(MATCHED_DOCUMENT_UUID, permanent));
    }

    @Test
    void shouldGenerateHashTokenWhenCaseIdIsPresent() {
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_UUID).permissions(permissionsList).build();
        when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),
                                                                      any(UUID.class))).thenReturn(Optional.of(doc));
        mockitoWhenRestExchangeThenThrow(
            initialiseMetaDataMap(null, "BEFTA_CASETYPE_2_2", "BEFTA_JURISDICTION_2"),
            HttpStatus.OK
        );
        String result =
            sut.generateHashToken(UUID.fromString(MATCHED_DOCUMENT_ID), getStoredDocumentResource());
        assertNotNull(result);
    }

    @Test
    void shouldGenerateHashTokenWhenCaseIdIsNotPresent() {
        final List<Permission> permissionsList = List.of(Permission.READ);
        final DocumentPermissions doc = DocumentPermissions.builder()
            .id(MATCHED_DOCUMENT_UUID)
            .permissions(permissionsList)
            .build();
        when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
            .thenReturn(Optional.of(doc));
        mockitoWhenRestExchangeThenThrow(
            initialiseMetaDataMap(null, "BEFTA_CASETYPE_2_2", "BEFTA_JURISDICTION_2"),
            HttpStatus.OK
        );

        String result =
            sut.generateHashToken(MATCHED_DOCUMENT_UUID, getStoredDocumentResource());
        assertNotNull(result);
    }

    @Test
    void checkUserPermissionTest3() {
        final List<Permission> permissionsList = List.of(Permission.READ);
        final DocumentPermissions doc = DocumentPermissions.builder()
            .id(MATCHED_DOCUMENT_UUID)
            .permissions(permissionsList)
            .build();
        when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(Optional.of(doc));

        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("1234567890123456", "", ""),
                                         HttpStatus.OK);
        Optional<StoredDocumentHalResource> documentMetadata = sut.getDocumentMetadata(MATCHED_DOCUMENT_UUID);

        assertThrows(ForbiddenException.class, () -> sut.checkUserPermission(documentMetadata.get(),
                                                                             UUID.fromString(MATCHED_DOCUMENT_ID),
                                                                             Permission.CREATE,
                                                                             USER_PERMISSION_ERROR,
                                                                             "exception string"));
    }

    private void mockAuthorisedServices() {
        List<String> caseTypeIds = new ArrayList<>();
        caseTypeIds.add("BEFTA_CASETYPE_1_1");
        caseTypeIds.add("BEFTA_CASETYPE_2_1");

        AuthorisedService authorisedService = AuthorisedService.builder()
            .id(XUI_WEBAPP)
            .caseTypeId(caseTypeIds)
            .build();

        List<AuthorisedService> authorisedServicesList = new ArrayList<>();
        authorisedServicesList.add(authorisedService);

        when(authorisedServicesMock.getAuthServices())
            .thenReturn(authorisedServicesList);
    }

    private StoredDocumentHalResource initialiseMetaDataMap(String caseId, String caseTypeId, String jurisdictionId) {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId", caseId);
        myMap.put("caseTypeId", caseTypeId);
        myMap.put("jurisdictionId", jurisdictionId);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMap);
        return storedDocumentHalResource;
    }

    private StoredDocumentHalResource getStoredDocumentResource() {

        Map<String, String> metaData = new HashMap<>();
        metaData.put(CASE_ID, CASE_ID);
        metaData.put(CASE_TYPE_ID, BEFTA_CASETYPE_2);
        metaData.put(JURISDICTION_ID, BEFTA_JURISDICTION_2);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setClassification(StoredDocumentHalResource.ClassificationEnum.PUBLIC);
        storedDocumentHalResource.setCreatedBy(USER_ID);
        storedDocumentHalResource.setCreatedOn(Date.from(Instant.now()));
        storedDocumentHalResource.setLastModifiedBy(USER_ID);
        storedDocumentHalResource.setMetadata(metaData);

        storedDocumentHalResource.addLinks(UUID.fromString(MATCHED_DOCUMENT_ID));

        return storedDocumentHalResource;
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
}
