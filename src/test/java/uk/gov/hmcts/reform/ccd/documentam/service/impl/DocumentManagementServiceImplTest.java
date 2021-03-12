package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.documentam.service.ValidationUtils;
import uk.gov.hmcts.reform.ccd.documentam.util.ApplicationUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PATCH;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.BINARY;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.BULK_SCAN_PROCESSOR;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_LENGTH;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DOCUMENTS;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.EMBEDDED;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.HREF;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.LINKS;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.ORIGINAL_FILE_NAME;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SELF;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_PERMISSION_ERROR;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.USER_PERMISSION_ERROR;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.USERID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.XUI_WEBAPP;

@RunWith(MockitoJUnitRunner.class)
class DocumentManagementServiceImplTest {

    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";
    private static final String CASE_ID = "1582550122096256";
    private static final String BEFTA_CASETYPE_2 = "BEFTA_CASETYPE_2";
    private static final String BEFTA_JURISDICTION_2 = "BEFTA_JURISDICTION_2";
    private static final String USER_ID = "userId";
    private final RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);
    private final SecurityUtils securityUtilsMock = mock(SecurityUtils.class);
    private final CaseDataStoreService caseDataStoreServiceMock = mock(CaseDataStoreService.class);
    private final UUID matchedDocUUID = UUID.fromString(MATCHED_DOCUMENT_ID);
    private final transient String serviceAuthorization = "auth";
    private final HttpEntity<?> requestEntityGlobal = new HttpEntity<>(getHttpHeaders());
    @InjectMocks
    private final DocumentManagementServiceImpl sut = new DocumentManagementServiceImpl(restTemplateMock,
                                                                                        securityUtilsMock,
                                                                                        caseDataStoreServiceMock,
                                                                                        new ValidationUtils());

    private final String documentURL = "http://localhost:4506";
    private final String documentTTL = "600000";
    private final String salt = "AAAOA7A2AA6AAAA5";

    @Test
    void documentMetadataInstantiation() {
        assertNotNull(sut);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sut, "documentTtl", "600000");
        ReflectionTestUtils.setField(sut, "documentURL", "http://localhost:4506");
        ReflectionTestUtils.setField(sut, "salt", "AAAOA7A2AA6AAAA5");

        final HttpHeaders headers = new HttpHeaders();
        headers.add(SERVICE_AUTHORIZATION, "123");
        when(securityUtilsMock.serviceAuthorizationHeaders()).thenReturn(headers);
        UserInfo userInfo = UserInfo.builder().uid("123").build();
        when(securityUtilsMock.getUserInfo()).thenReturn(userInfo);
    }

    @Test
    void getDocumentMetadata_HappyPath() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_ServiceException() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, requestEntityGlobal,
            StoredDocumentHalResource.class))
               .thenThrow(HttpClientErrorException.create("woopsie", HttpStatus.BAD_GATEWAY, "404",
                                                          new HttpHeaders(),
                                                          new byte[1],
                                                          Charset.defaultCharset()));

        assertThrows(ServiceException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });

        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_ResourceNotFoundException() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.BAD_REQUEST);
        assertThrows(ResourceNotFoundException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
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
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, requestEntityGlobal,
            StoredDocumentHalResource.class))
               .thenThrow(httpClientErrorException);
        assertThrows(ResourceNotFoundException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_ForbiddenException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.FORBIDDEN,
                                                                                            "woopsie",
                                                                                            new HttpHeaders(),
                                                                                            null,
                                                                                            null);
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, requestEntityGlobal,
            StoredDocumentHalResource.class))
               .thenThrow(httpClientErrorException);
        assertThrows(ForbiddenException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_BadRequestException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.BAD_REQUEST,
                                                                                            "woopsie",
                                                                                            new HttpHeaders(),
                                                                                            null,
                                                                                            null);
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, requestEntityGlobal,
            StoredDocumentHalResource.class))
               .thenThrow(httpClientErrorException);
        assertThrows(BadRequestException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_ServiceException() {
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, requestEntityGlobal,
            StoredDocumentHalResource.class))
               .thenThrow(HttpClientErrorException.class);

        assertThrows(ServiceException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void extractCaseIdFromMetadata_HappyPath() {
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("1234qwer1234qwer", "", ""),
                                         HttpStatus.OK);
        ResponseEntity<StoredDocumentHalResource> responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        String caseId = sut.extractCaseIdFromMetadata(responseEntity.getBody());
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

        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET,
            requestEntityGlobal,
            ByteArrayResource.class
                                              )).thenReturn(new ResponseEntity<ByteArrayResource>(headers,
                                                                                                  HttpStatus.OK));
        ResponseEntity responseEntity = sut.getDocumentBinaryContent(matchedDocUUID);
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

        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET,
            requestEntityGlobal,
            ByteArrayResource.class
                                              )).thenThrow(HttpClientErrorException.create("woopsie",
                                                                                           HttpStatus.BAD_GATEWAY,
                                                                                           "404",
                                                                                           new HttpHeaders(),
                                                                                           new byte[1],
                                                                                           Charset.defaultCharset()));

        assertThrows(ServiceException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Try_ResponseNotOK() {
        ByteArrayResource byteArrayResource = mock(ByteArrayResource.class);
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET, requestEntityGlobal, ByteArrayResource.class))
               .thenReturn(new ResponseEntity<ByteArrayResource>(byteArrayResource, HttpStatus.BAD_REQUEST));
        ResponseEntity responseEntity = sut.getDocumentBinaryContent(matchedDocUUID);
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

        assertThrows(ResourceNotFoundException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

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

        assertThrows(ForbiddenException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

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

        assertThrows(BadRequestException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

        verifyRestExchangeByteArray();
    }

    private void mockitoWhenRestExchangeByteArrayThenThrow(HttpClientErrorException httpClientErrorException) {
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET, requestEntityGlobal,
            ByteArrayResource.class))
               .thenThrow(httpClientErrorException);
    }

    @Test
    void getDocumentBinaryContent_Throws_HttpClientErrorException_ServiceException() {
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET, requestEntityGlobal,
            ByteArrayResource.class))
               .thenThrow(HttpClientErrorException.class);

        assertThrows(ServiceException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

        verifyRestExchangeByteArray();
    }

    @Test
    void checkServicePermission_WhenJurisdictionIdIsNull() {
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("", "caseTypeId",
                                                               ""), HttpStatus.OK);
        assertThrows(NullPointerException.class, () ->
            sut.checkServicePermission(new ResponseEntity<>(HttpStatus.OK),
                                       XUI_WEBAPP,
                                       Permission.READ,
                                       "log string",
                                       "exception string"));
    }

    private StoredDocumentHalResource initialiseMetaData(String caseTypeId, String jurisdictionID) {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseTypeId", caseTypeId);
        myMap.put("jurisdictionId", jurisdictionID);
        storedDocumentHalResource.setMetadata(myMap);
        return storedDocumentHalResource;
    }

    @Test
    void checkServicePermissionForUpload_WhenServiceIsNotAuthorised() {
        assertThrows(ForbiddenException.class, () -> sut.checkServicePermissionsForUpload(
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
        assertThrows(ForbiddenException.class, () -> sut.checkServicePermissionsForUpload(
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
        assertThrows(ForbiddenException.class, () -> sut.checkServicePermissionsForUpload(
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
            new ResponseEntity<>(HttpStatus.OK),
            "bad_Service_name",
            Permission.READ,
            "log string",
            "exception string"));
    }

    @Test
    void checkUserPermission_Throws_CaseNotFoundException() {
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("1234567890123456",
                                                               "", ""), HttpStatus.OK);
        ResponseEntity<StoredDocumentHalResource> responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(null);

        assertThrows(Exception.class, () -> {
            sut.checkUserPermission(responseEntity,
                                    matchedDocUUID,
                                    Permission.READ,
                                    USER_PERMISSION_ERROR,
                                    "exception string");
        });

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    @Test
    void checkUserPermission_Throws_InvalidCaseId_BadRequestException() {
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("123456789012345@",
                                                               "", ""), HttpStatus.OK);
        ResponseEntity<StoredDocumentHalResource> responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        assertThrows(BadRequestException.class, () ->
            sut.checkUserPermission(responseEntity,
                                    matchedDocUUID,
                                    Permission.READ,
                                    USER_PERMISSION_ERROR,
                                    "exception string"));

        verifyRestExchangeOnStoredDoc();
    }


    @Test
    void checkUserPermission_ReturnsFalse_Scenario1() {
        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("1234567890123456",
                                                               "", ""), HttpStatus.OK);
        ResponseEntity<StoredDocumentHalResource> responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<Permission> permissionsList = new ArrayList<>();
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(Optional.ofNullable(doc));

        assertThrows(ForbiddenException.class, () -> sut.checkUserPermission(responseEntity,
                                                                             matchedDocUUID,
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
        ResponseEntity<StoredDocumentHalResource> responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<Permission> permissionsList = new ArrayList<>();
        DocumentPermissions doc;
        doc =
            DocumentPermissions.builder().id("40000a2b-00ce-00eb-0068-2d00a700be9c").permissions(permissionsList)
                               .build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(Optional.ofNullable(doc));

        assertThrows(ForbiddenException.class, () -> sut.checkUserPermission(responseEntity,
                                                                             matchedDocUUID,
                                                                             Permission.READ,
                                                                             USER_PERMISSION_ERROR,
                                                                             "exception string"));

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    private void mockitoWhenRestExchangeThenThrow(StoredDocumentHalResource storedDocumentHalResource,
                                                  HttpStatus httpStatus) {
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET, requestEntityGlobal,
            StoredDocumentHalResource.class))
               .thenReturn(new ResponseEntity<>(storedDocumentHalResource, httpStatus));
    }

    private void verifyRestExchangeOnStoredDoc() {
        verify(restTemplateMock, times(1))
            .exchange(documentURL + "/documents/" + MATCHED_DOCUMENT_ID, HttpMethod.GET, requestEntityGlobal,
                      StoredDocumentHalResource.class);
    }

    private void verifyRestExchangeByteArray() {
        verify(restTemplateMock, times(1))
            .exchange(documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary", HttpMethod.GET,
                      requestEntityGlobal, ByteArrayResource.class);
    }

    private void verifyCaseDataServiceGetDocMetadata() {
        verify(caseDataStoreServiceMock, times(1))
            .getCaseDocumentMetadata(anyString(), any(UUID.class));
    }

    @Test
    void patchDocumentMetadata_HappyPath() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2)
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
                                                                           .caseId(CASE_ID)
                                                                           .caseTypeId(BEFTA_CASETYPE_2)
                                                                           .jurisdictionId(BEFTA_JURISDICTION_2)
                                                                           .documentHashTokens(documentList)
                                                                           .build();

        ResponseEntity responseEntity = sut.patchDocumentMetadata(caseDocumentsMetadata);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }


    @Test
    void patchDocumentMetadata_Throws_NotFoundException() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

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
               .thenThrow(
                   HttpClientErrorException.create(HttpStatus.NOT_FOUND, "woopsie",
                                                   new HttpHeaders(), null, null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID)
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
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

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
               .thenThrow(
                   HttpClientErrorException.create(HttpStatus.FORBIDDEN, "woopsie",
                                                   new HttpHeaders(), null, null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID)
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
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

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
                                                                           .documentHashTokens(documentList)
                                                                           .build();

        assertThrows(BadRequestException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    void patchDocumentMetadata_Throws_ForbiddenException_InvalidHashToken() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("caseId", CASE_ID);
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
               .thenThrow(
                   HttpClientErrorException.create(HttpStatus.FORBIDDEN, "woopsie",
                                                   new HttpHeaders(), null, null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID)
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
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
                                                 .hashToken(ApplicationUtils.generateHashCode(
                                                     salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2)
                                                         .concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

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
               .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_GATEWAY,
                                                          "woopsie", new HttpHeaders(), null,
                                                          null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                                           .caseId(CASE_ID)
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

        HashMap<String, String> binaryHash = new HashMap<>();
        HashMap<String, String> selfHash = new HashMap<>();
        selfHash.put(HREF, "http://localhost:4455/cases/documents/35471d43-0dad-42c1-b05a-4821028f50a2");
        binaryHash.put(HREF, "http://localhost:4455/cases/documents/35471d43-0dad-42c1-b05a-4821028f50a2/binary");

        LinkedHashMap<String, Object> linksLinkedHashMap = new LinkedHashMap<>();
        LinkedHashMap<String, Object> binarySelfLinkedHashMap = new LinkedHashMap<>();

        binarySelfLinkedHashMap.put(BINARY, binaryHash);
        binarySelfLinkedHashMap.put(SELF, selfHash);
        linksLinkedHashMap.put(LINKS, binarySelfLinkedHashMap);

        ArrayList arrayList = new ArrayList();
        arrayList.add(linksLinkedHashMap);

        LinkedHashMap<String, Object> documentsLinkedHashMap = new LinkedHashMap<>();
        documentsLinkedHashMap.put(DOCUMENTS, arrayList);

        LinkedHashMap<String, Object> embeddedLinkedHashMap = new LinkedHashMap<>();
        embeddedLinkedHashMap.put(EMBEDDED, documentsLinkedHashMap);

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any()))
               .thenReturn(new ResponseEntity<>(embeddedLinkedHashMap, HttpStatus.OK));

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        MockMultipartFile multipartFile = mock(MockMultipartFile.class);
        List<MultipartFile> files = new ArrayList<>();
        files.add(multipartFile);

        ResponseEntity<Object> responseEntity = sut.uploadDocuments(files, "classification",
                                                                    BEFTA_CASETYPE_2,
                                                                    BEFTA_JURISDICTION_2);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadDocuments_Throw_ServiceException() {

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any()))
               .thenThrow(HttpClientErrorException.create(
                   HttpStatus.BAD_GATEWAY, "woopsie", new HttpHeaders(), null, null));

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        assertThrows(ServiceException.class, () -> {
            sut.uploadDocuments(
                files,
                "classification",
                BEFTA_CASETYPE_2,
                BEFTA_JURISDICTION_2);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadDocuments_Throw_ForbiddenException() {

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any()))
               .thenThrow(HttpClientErrorException.create(
                   HttpStatus.FORBIDDEN, "woopsie", new HttpHeaders(), null, null));

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        assertThrows(ForbiddenException.class, () -> {
            sut.uploadDocuments(
                files,
                "classification",
                BEFTA_CASETYPE_2,
                BEFTA_JURISDICTION_2);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadDocuments_Throw_BadRequestException() {

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any()))
               .thenThrow(HttpClientErrorException.create(
                   HttpStatus.BAD_REQUEST, "woopsie", new HttpHeaders(), null, null));

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        assertThrows(BadRequestException.class, () -> {
            sut.uploadDocuments(
                files,
                "classification",
                BEFTA_CASETYPE_2,
                BEFTA_JURISDICTION_2);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadDocuments_Throw_NotFoundException() {

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any()))
               .thenThrow(HttpClientErrorException.create(
                   HttpStatus.NOT_FOUND, "woopsie", new HttpHeaders(), null, null));

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        assertThrows(ResourceNotFoundException.class, () -> {
            sut.uploadDocuments(
                files,
                "classification",
                BEFTA_CASETYPE_2,
                BEFTA_JURISDICTION_2);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadDocuments_NoLinksExceptionThrow() {

        HashMap<String, String> binaryHash = new HashMap<>();
        HashMap<String, String> selfHash = new HashMap<>();
        selfHash.put(HREF, "http://localhost:4455/cases/documents/35471d43-0dad-42c1-b05a-4821028f50a2");
        binaryHash.put(HREF, "http://localhost:4455/cases/documents/35471d43-0dad-42c1-b05a-4821028f50a2/binary");

        LinkedHashMap<String, Object> linksLinkedHashMap = new LinkedHashMap<>();

        ArrayList arrayList = new ArrayList();
        arrayList.add(linksLinkedHashMap);

        LinkedHashMap<String, Object> documentsLinkedHashMap = new LinkedHashMap<>();
        documentsLinkedHashMap.put(DOCUMENTS, arrayList);

        LinkedHashMap<String, Object> embeddedLinkedHashMap = new LinkedHashMap<>();
        embeddedLinkedHashMap.put(EMBEDDED, documentsLinkedHashMap);

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any()))
               .thenReturn(new ResponseEntity<>(embeddedLinkedHashMap, HttpStatus.OK));

        List<MultipartFile> files = new ArrayList<>();

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThrows(ResponseFormatException.class, () -> {
            sut.uploadDocuments(files, "classification", BEFTA_CASETYPE_2, BEFTA_JURISDICTION_2);
        });
    }

    private String getEffectiveTTL() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        return format.format(new Timestamp(new Date().getTime() + Long.parseLong(documentTTL)));
    }

    @Test
    void patchDocument_HappyPath() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand,
                                                                                 getHttpHeaders());
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
                                      )).thenReturn(new ResponseEntity<>(storedDocumentHalResource, HttpStatus.OK));

        ResponseEntity responseEntity = sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    void patchDocument_ResourceNotFound() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand,
                                                                                 getHttpHeaders());
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
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        });
    }

    @Test
    void patchDocument_BadRequest() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand,
                                                                                 getHttpHeaders());
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
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        });
    }

    @Test
    void patchDocument_Forbidden() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand,
                                                                                 getHttpHeaders());
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
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        });
    }

    @Test
    void patchDocument_HttpClientErrorException() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand,
                                                                                 getHttpHeaders());
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
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand));
    }

    @Test
    void patchDocument_ServiceException() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand,
                                                                                 getHttpHeaders());
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
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        });
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(SERVICE_AUTHORIZATION, "123");
        headers.set(USERID, "123");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void patchDocument_BadRequestTTL() {
        List<String> roles = new ArrayList<>();
        roles.add("Role");
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        updateDocumentCommand.setTtl("600000");
        assertThrows(BadRequestException.class, () -> {
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_HappyPath() {
        Boolean permanent = true;
        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=%s", documentURL, MATCHED_DOCUMENT_ID,
                                                 permanent);

        ResponseEntity<HttpStatus> responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        ResponseEntity deletionResponse = new ResponseEntity<>(responseEntity, HttpStatus.NO_CONTENT);
        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            HttpStatus.class
                                      )).thenReturn(deletionResponse);

        responseEntity = sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }


    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_NotFoundException() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL,
                                                 MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            HttpStatus.class
                                      )).thenThrow(HttpClientErrorException.NotFound.create("woopsie",
                                                                                            HttpStatus.NOT_FOUND,
                                                                                            "404",
                                                                                            new HttpHeaders(),
                                                                                            new byte[1],
                                                                                            Charset.defaultCharset()));

        assertThrows(ResourceNotFoundException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_ForbiddenException() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL,
                                                 MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            HttpStatus.class
                                      )).thenThrow(HttpClientErrorException.NotFound.create("woopsie",
                                                                                            HttpStatus.FORBIDDEN,
                                                                                            "404",
                                                                                            new HttpHeaders(),
                                                                                            new byte[1],
                                                                                            Charset.defaultCharset()));

        assertThrows(ForbiddenException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_BadRequestException() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL,
                                                 MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            HttpStatus.class
                                      )).thenThrow(HttpClientErrorException.NotFound.create("woopsie",
                                                                                            HttpStatus.BAD_REQUEST,
                                                                                            "404",
                                                                                            new HttpHeaders(),
                                                                                            new byte[1],
                                                                                            Charset.defaultCharset()));

        assertThrows(BadRequestException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_ServiceException() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL,
                                                 MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            HttpStatus.class
                                      )).thenThrow(HttpClientErrorException.NotFound.create("woopsie",
                                                                                            HttpStatus.BAD_GATEWAY,
                                                                                            "404",
                                                                                            new HttpHeaders(),
                                                                                            new byte[1],
                                                                                            Charset.defaultCharset()));

        assertThrows(ServiceException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_ResourceNotFound() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL,
                                                 MATCHED_DOCUMENT_ID);

        ResponseEntity<HttpStatus> responseEntity = new ResponseEntity<>(HttpStatus.ACCEPTED);
        ResponseEntity deletionResponse = new ResponseEntity<>(responseEntity, HttpStatus.NOT_FOUND);
        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            HttpStatus.class)).thenReturn(deletionResponse);

        assertThrows(ResourceNotFoundException.class, () ->
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent));
    }

    @Test
    void checkUserPermissionTest2() {
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(Optional.of(doc));

        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("!!VCB12", "", ""),
                                         HttpStatus.OK);
        ResponseEntity<StoredDocumentHalResource> responseEntity = sut.getDocumentMetadata(matchedDocUUID);

        assertThrows(BadRequestException.class, () ->
            sut.checkUserPermission(responseEntity,
                                    UUID.fromString(MATCHED_DOCUMENT_ID),
                                    Permission.CREATE,
                                    USER_PERMISSION_ERROR,
                                    "exception string"));
    }

    @Test
    void shouldGenerateHashTokenWhenCaseIdIsPresent() {
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),
                                                                      any(UUID.class))).thenReturn(Optional.of(doc));
        mockitoWhenRestExchangeThenThrow(
            initialiseMetaDataMap(null, "BEFTA_CASETYPE_2_2", "BEFTA_JURISDICTION_2"),
            HttpStatus.OK
        );
        String result = sut.generateHashToken(UUID.fromString(MATCHED_DOCUMENT_ID));
        assertNotNull(result);
    }

    @Test
    void shouldGenerateHashTokenWhenCaseIdIsNotPresent() {
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),
                                                                      any(UUID.class))).thenReturn(Optional.of(doc));
        mockitoWhenRestExchangeThenThrow(
            initialiseMetaDataMap(null, "BEFTA_CASETYPE_2_2", "BEFTA_JURISDICTION_2"),
            HttpStatus.OK
        );

        String result = sut.generateHashToken(UUID.fromString(MATCHED_DOCUMENT_ID));
        assertNotNull(result);
    }

    @Test
    void checkUserPermissionTest3() {
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(), any(UUID.class)))
               .thenReturn(Optional.of(doc));

        mockitoWhenRestExchangeThenThrow(initialiseMetaDataMap("1234567890123456", "", ""),
                                         HttpStatus.OK);
        ResponseEntity<StoredDocumentHalResource> responseEntity = sut.getDocumentMetadata(matchedDocUUID);

        ;
        assertThrows(ForbiddenException.class, () -> sut.checkUserPermission(responseEntity,
                                                                             UUID.fromString(MATCHED_DOCUMENT_ID),
                                                                             Permission.CREATE,
                                                                             USER_PERMISSION_ERROR,
                                                                             "exception string"));
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

}
