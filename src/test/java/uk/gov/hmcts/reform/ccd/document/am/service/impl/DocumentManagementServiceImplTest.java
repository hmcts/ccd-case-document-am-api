package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import org.junit.jupiter.api.Assertions;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.util.ApplicationUtils;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PATCH;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.BINARY;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.BULK_SCAN_PROCESSOR;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_LENGTH;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DOCUMENTS;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.EMBEDDED;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.HREF;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.LINKS;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ORIGINAL_FILE_NAME;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SELF;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.USERID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.XUI_WEBAPP;

@RunWith(MockitoJUnitRunner.class)
class DocumentManagementServiceImplTest {

    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";

    private transient String serviceAuthorization = "auth";
    private static final String CASE_ID = "1582550122096256";
    private static final String BEFTA_CASETYPE_2 =  "BEFTA_CASETYPE_2";
    private static final String BEFTA_JURISDICTION_2 =  "BEFTA_JURISDICTION_2";
    private static final String USER_ID =  "userId";

    private RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);
    private SecurityUtils securityUtilsMock = mock(SecurityUtils.class);
    private CaseDataStoreService caseDataStoreServiceMock = mock(CaseDataStoreService.class);

    private HttpEntity<?> requestEntityGlobal  = new HttpEntity<>(getHttpHeaders());
    private UUID matchedDocUUID = UUID.fromString(MATCHED_DOCUMENT_ID);

    @InjectMocks
    private DocumentManagementServiceImpl sut = new DocumentManagementServiceImpl(restTemplateMock, securityUtilsMock,

                                                                                  caseDataStoreServiceMock);

    private String documentURL = "http://localhost:4506";
    private String documentTTL = "600000";
    private String salt = "AAAOA7A2AA6AAAA5";

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
        when(securityUtilsMock.getUserId()).thenReturn("123");
    }

    @Test
    void getDocumentMetadata_HappyPath() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());

        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_ServiceException() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET,requestEntityGlobal,
            StoredDocumentHalResource.class))
            .thenThrow(HttpClientErrorException.create("woopsie", HttpStatus.BAD_GATEWAY, "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        Assertions.assertThrows(ServiceException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });

        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_ResourceNotFoundException() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.BAD_REQUEST);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_ResourceNotFoundException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.NOT_FOUND,"woopsie", new HttpHeaders(),null,null);
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET,requestEntityGlobal,
            StoredDocumentHalResource.class))
            .thenThrow(httpClientErrorException);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_ForbiddenException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.FORBIDDEN,"woopsie", new HttpHeaders(),null,null);
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET,requestEntityGlobal,
            StoredDocumentHalResource.class))
            .thenThrow(httpClientErrorException);
        Assertions.assertThrows(ForbiddenException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_BadRequestException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.BAD_REQUEST,"woopsie", new HttpHeaders(),null,null);
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET,requestEntityGlobal,
            StoredDocumentHalResource.class))
            .thenThrow(httpClientErrorException);
        Assertions.assertThrows(BadRequestException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void getDocumentMetadata_Throws_HttpClientErrorException_ServiceException() {
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET,requestEntityGlobal,
            StoredDocumentHalResource.class))
            .thenThrow(HttpClientErrorException.class);

        Assertions.assertThrows(ServiceException.class, () -> {
            sut.getDocumentMetadata(matchedDocUUID);
        });
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void extractCaseIdFromMetadata_HappyPath() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234qwer1234qwer");
        storedDocumentHalResource.setMetadata(myMap);
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        String caseId = sut.extractCaseIdFromMetadata(responseEntity.getBody());
        assertEquals("1234qwer1234qwer", caseId);
        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void extractCaseIdFromMetadata_InvalidObjectType() {
        String response = sut.extractCaseIdFromMetadata("wrong object");
        assertNull(response);
    }

    @Test
    void getDocumentBinaryContent_HappyPath() {
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
        )).thenReturn(new ResponseEntity<ByteArrayResource>(headers, HttpStatus.OK));
        ResponseEntity responseEntity = sut.getDocumentBinaryContent(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        assertEquals(responseEntity.getHeaders(),headers);

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
        )).thenThrow(HttpClientErrorException.create("woopsie", HttpStatus.BAD_GATEWAY, "404", new HttpHeaders(), new byte[1],
                                                    Charset.defaultCharset()));

        Assertions.assertThrows(ServiceException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Try_ResponseNotOK() {
        ByteArrayResource byteArrayResource = mock(ByteArrayResource.class);
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET,requestEntityGlobal, ByteArrayResource.class))
            .thenReturn(new ResponseEntity<ByteArrayResource>(byteArrayResource,HttpStatus.BAD_REQUEST));
        ResponseEntity responseEntity = sut.getDocumentBinaryContent(matchedDocUUID);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Throws_HttpClientErrorException_ResourceNotFoundException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.NOT_FOUND,"woopsie", new HttpHeaders(),null,null);
        mockitoWhenRestExchangeByteArrayThenThrow(httpClientErrorException);

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Throws_HttpClientErrorException_ForbiddenException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.FORBIDDEN,"woopsie", new HttpHeaders(),null,null);
        mockitoWhenRestExchangeByteArrayThenThrow(httpClientErrorException);

        Assertions.assertThrows(ForbiddenException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Throws_HttpClientErrorException_BadRequestException() {
        HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatus.BAD_REQUEST,"woopsie", new HttpHeaders(),null,null);
        mockitoWhenRestExchangeByteArrayThenThrow(httpClientErrorException);

        Assertions.assertThrows(BadRequestException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

        verifyRestExchangeByteArray();
    }

    private void mockitoWhenRestExchangeByteArrayThenThrow(HttpClientErrorException httpClientErrorException) {
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET,requestEntityGlobal,
            ByteArrayResource.class))
            .thenThrow(httpClientErrorException);
    }

    @Test
    void getDocumentBinaryContent_Throws_HttpClientErrorException_ServiceException() {
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET,requestEntityGlobal,
            ByteArrayResource.class))
            .thenThrow(HttpClientErrorException.class);

        Assertions.assertThrows(ServiceException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

        verifyRestExchangeByteArray();
    }


    @Test
    void checkUserPermission_HappyPath() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234567890123456");
        storedDocumentHalResource.setMetadata(myMap);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class))).thenReturn(Optional.of(doc));

        Boolean result = sut.checkUserPermission(responseEntity, matchedDocUUID, Permission.READ);
        assertEquals(Boolean.TRUE, result);

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    @Test
    void checkServicePermission_HappyPath() {
        when(securityUtilsMock.getServiceId()).thenReturn("xui_webapp");
        mockitoWhenRestExchangeThenThrow(initialiseMetaData("caseTypeId", "BEFTA_JURISDICTION_2"), HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        Boolean result = sut.checkServicePermission(responseEntity, Permission.READ);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void checkServicePermission_WhenServiceIsNotAuthorised() {
        when(securityUtilsMock.getServiceId()).thenReturn(BULK_SCAN_PROCESSOR);
        mockitoWhenRestExchangeThenThrow(initialiseMetaData("caseTypeId", "BEFTA_JURISDICTION_2"), HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        Boolean result = sut.checkServicePermission(responseEntity, Permission.READ);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void checkServicePermission_WhenCaseTypeIsNull() {
        when(securityUtilsMock.getServiceId()).thenReturn(XUI_WEBAPP);
        mockitoWhenRestExchangeThenThrow(initialiseMetaData("", "BEFTA_JURISDICTION_2"), HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);

        Boolean result = sut.checkServicePermission(responseEntity, Permission.READ);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void checkServicePermission_WhenJurisdictionIdIsNull() {
        when(securityUtilsMock.getServiceId()).thenReturn(XUI_WEBAPP);
        mockitoWhenRestExchangeThenThrow(initialiseMetaData("caseTypeId", ""), HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);

        Boolean result = sut.checkServicePermission(responseEntity, Permission.READ);
        assertEquals(Boolean.FALSE, result);
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
    void checkServicePermissionForUpload_HappyPath() {
        when(securityUtilsMock.getServiceId()).thenReturn("xui_webapp");
        Boolean result = sut.checkServicePermissionsForUpload("caseTypeId", "BEFTA_JURISDICTION_2", Permission.READ
        );
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void checkServicePermissionForUpload_WhenServiceIsNotAuthorised() {
        when(securityUtilsMock.getServiceId()).thenReturn(BULK_SCAN_PROCESSOR);
        Boolean result = sut.checkServicePermissionsForUpload("caseTypeId", "BEFTA_JURISDICTION_2", Permission.READ
        );
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void checkServicePermissionForUpload_WhenCaseTypeIsNull() {
        when(securityUtilsMock.getServiceId()).thenReturn(BULK_SCAN_PROCESSOR);
        Boolean result = sut.checkServicePermissionsForUpload("", "BEFTA_JURISDICTION_2", Permission.READ
        );
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void checkServicePermissionForUpload_WhenJurisdictionIdIsNull() {
        when(securityUtilsMock.getServiceId()).thenReturn(BULK_SCAN_PROCESSOR);
        Boolean result = sut.checkServicePermissionsForUpload("caseTypeId", "", Permission.READ
        );
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void checkUserPermission_Throws_CaseNotFoundException() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234567890123456");
        storedDocumentHalResource.setMetadata(myMap);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class)))
            .thenReturn(null);

        Assertions.assertThrows(Exception.class, () -> {
            sut.checkUserPermission(responseEntity, matchedDocUUID, Permission.READ);
        });

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    @Test
    void checkUserPermission_Throws_InvalidCaseId_BadRequestException() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","123456789012345@");
        storedDocumentHalResource.setMetadata(myMap);
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        Assertions.assertThrows(BadRequestException.class, () -> {
            sut.checkUserPermission(responseEntity, matchedDocUUID, Permission.READ);
        });

        verifyRestExchangeOnStoredDoc();
    }


    @Test
    void checkUserPermission_ReturnsFalse_Scenario1() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234567890123456");
        storedDocumentHalResource.setMetadata(myMap);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<Permission> permissionsList = new ArrayList<>();
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class)))
            .thenReturn(Optional.ofNullable(doc));

        Boolean result = sut.checkUserPermission(responseEntity, matchedDocUUID, Permission.READ);
        assertEquals(Boolean.FALSE, result);

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }


    @Test
    void checkUserPermission_ReturnsFalse_Scenario2() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234567890123456");
        storedDocumentHalResource.setMetadata(myMap);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<Permission> permissionsList = new ArrayList<>();
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id("40000a2b-00ce-00eb-0068-2d00a700be9c").permissions(permissionsList).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class)))
            .thenReturn(Optional.ofNullable(doc));

        Boolean result = sut.checkUserPermission(responseEntity, matchedDocUUID, Permission.READ);
        assertEquals(Boolean.FALSE, result);

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    private void mockitoWhenRestExchangeThenThrow(StoredDocumentHalResource storedDocumentHalResource, HttpStatus httpStatus) {
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/documents/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET,requestEntityGlobal,
            StoredDocumentHalResource.class))
            .thenReturn(new ResponseEntity<>(storedDocumentHalResource,httpStatus));
    }

    private void verifyRestExchangeOnStoredDoc() {
        verify(restTemplateMock, times(1))
            .exchange(documentURL + "/documents/" + MATCHED_DOCUMENT_ID,HttpMethod.GET,requestEntityGlobal, StoredDocumentHalResource.class);
    }

    private void verifyRestExchangeByteArray() {
        verify(restTemplateMock, times(1))
            .exchange(documentURL + "/documents/" + MATCHED_DOCUMENT_ID + "/binary",HttpMethod.GET,requestEntityGlobal, ByteArrayResource.class);
    }

    private void verifyCaseDataServiceGetDocMetadata() {
        verify(caseDataStoreServiceMock, times(1))
            .getCaseDocumentMetadata(anyString(),any(UUID.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchDocumentMetadata_HappyPath() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
            .hashToken(ApplicationUtils.generateHashCode(salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2).concat(BEFTA_CASETYPE_2))).build();
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
    @SuppressWarnings("unchecked")
    void patchDocumentMetadata_Throws_NotFoundException() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
            .hashToken(ApplicationUtils.generateHashCode(salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2).concat(BEFTA_CASETYPE_2))).build();
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
            .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND,"woopsie", new HttpHeaders(),null,null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID)
            .caseTypeId(BEFTA_CASETYPE_2)
            .jurisdictionId(BEFTA_JURISDICTION_2)
            .documentHashTokens(documentList)
            .build();

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchDocumentMetadata_Throws_ForbiddenException() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
            .hashToken(ApplicationUtils.generateHashCode(salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2).concat(BEFTA_CASETYPE_2))).build();
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
            .thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN,"woopsie", new HttpHeaders(),null,null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID)
            .caseTypeId(BEFTA_CASETYPE_2)
            .jurisdictionId(BEFTA_JURISDICTION_2)
            .documentHashTokens(documentList)
            .build();

        Assertions.assertThrows(ForbiddenException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchDocumentMetadata_Throws_BadRequestException() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
            .hashToken(ApplicationUtils.generateHashCode(salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2).concat(BEFTA_CASETYPE_2))).build();
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
            .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST,"woopsie", new HttpHeaders(),null,null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID)
            .caseTypeId(BEFTA_CASETYPE_2)
            .jurisdictionId(BEFTA_JURISDICTION_2)
            .documentHashTokens(documentList)
            .build();

        Assertions.assertThrows(BadRequestException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchDocumentMetadata_Throws_ForbiddenException_InvalidHashToken() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
            .hashToken(ApplicationUtils.generateHashCode(salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2).concat(BEFTA_CASETYPE_2))).build();
        List<DocumentHashToken> documentList = new ArrayList<>();
        documentList.add(doc);

        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("caseId",CASE_ID);
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
            .thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN,"woopsie", new HttpHeaders(),null,null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID)
            .caseTypeId(BEFTA_CASETYPE_2)
            .jurisdictionId(BEFTA_JURISDICTION_2)
            .documentHashTokens(documentList)
            .build();

        Assertions.assertThrows(ForbiddenException.class, () -> {
            sut.patchDocumentMetadata(caseDocumentsMetadata);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchDocumentMetadata_Throws_BadGatewayException() {
        DocumentHashToken doc = DocumentHashToken.builder().id(MATCHED_DOCUMENT_ID)
            .hashToken(ApplicationUtils.generateHashCode(salt.concat(MATCHED_DOCUMENT_ID).concat(BEFTA_JURISDICTION_2).concat(BEFTA_CASETYPE_2))).build();
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
            .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_GATEWAY,"woopsie", new HttpHeaders(),null,null));

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID)
            .caseTypeId(BEFTA_CASETYPE_2)
            .jurisdictionId(BEFTA_JURISDICTION_2)
            .documentHashTokens(documentList)
            .build();

        Assertions.assertThrows(ServiceException.class, () -> {
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
        documentsLinkedHashMap.put(DOCUMENTS,arrayList);

        LinkedHashMap<String, Object> embeddedLinkedHashMap = new LinkedHashMap<>();
        embeddedLinkedHashMap.put(EMBEDDED,documentsLinkedHashMap);

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(embeddedLinkedHashMap, HttpStatus.OK));

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        List<String> collection = new ArrayList<String>();
        collection.add("string");
        ServiceAndUserDetails serviceAndUserDetails = new ServiceAndUserDetails(USER_ID,serviceAuthorization, collection,"servicename");
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication().getPrincipal()).thenReturn(serviceAndUserDetails);

        MockMultipartFile multipartFile = mock(MockMultipartFile.class);
        List<MultipartFile> files = new ArrayList<>();
        files.add(multipartFile);
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(USER_ID, "1234"));

        ResponseEntity<Object> responseEntity = sut.uploadDocuments(files,"classification", BEFTA_CASETYPE_2, BEFTA_JURISDICTION_2);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadDocuments_Throw_ServiceException() {

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any())).thenThrow(HttpClientErrorException.create(
            HttpStatus.BAD_GATEWAY,"woopsie", new HttpHeaders(),null,null));

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        Assertions.assertThrows(ServiceException.class, () -> {
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

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any())).thenThrow(HttpClientErrorException.create(
            HttpStatus.FORBIDDEN,"woopsie", new HttpHeaders(),null,null));

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        Assertions.assertThrows(ForbiddenException.class, () -> {
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

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any())).thenThrow(HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST,"woopsie", new HttpHeaders(),null,null));

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        Assertions.assertThrows(BadRequestException.class, () -> {
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

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any())).thenThrow(HttpClientErrorException.create(
            HttpStatus.NOT_FOUND,"woopsie", new HttpHeaders(),null,null));

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
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
        documentsLinkedHashMap.put(DOCUMENTS,arrayList);

        LinkedHashMap<String, Object> embeddedLinkedHashMap = new LinkedHashMap<>();
        embeddedLinkedHashMap.put(EMBEDDED,documentsLinkedHashMap);

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(embeddedLinkedHashMap, HttpStatus.OK));

        List<String> collection = new ArrayList<String>();
        collection.add("string");
        ServiceAndUserDetails serviceAndUserDetails = new ServiceAndUserDetails(USER_ID,serviceAuthorization, collection,"servicename");
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication().getPrincipal()).thenReturn(serviceAndUserDetails);

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Assertions.assertThrows(ResponseFormatException.class, () -> {
            sut.uploadDocuments(files,"classification", BEFTA_CASETYPE_2, BEFTA_JURISDICTION_2);
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
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand, getHttpHeaders());
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
        )).thenReturn(new ResponseEntity<>(storedDocumentHalResource, HttpStatus.OK));

        ResponseEntity responseEntity = sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
    }

    @Test
    void patchDocument_ResourceNotFound() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand, getHttpHeaders());
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
        )).thenReturn(new ResponseEntity<>(storedDocumentHalResource, HttpStatus.NOT_FOUND));

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        });
    }

    @Test
    void patchDocument_BadRequest() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand, getHttpHeaders());
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
        )).thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST,"woopsie", new HttpHeaders(),null,null));

        Assertions.assertThrows(BadRequestException.class, () -> {
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        });
    }

    @Test
    void patchDocument_Forbidden() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand, getHttpHeaders());
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
        )).thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN,"woopsie", new HttpHeaders(),null,null));

        Assertions.assertThrows(ForbiddenException.class, () -> {
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        });
    }

    @Test
    void patchDocument_HttpClientErrorException() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand, getHttpHeaders());
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
        )).thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.NOT_FOUND, "404", new HttpHeaders(), new byte[1],
                                                              Charset.defaultCharset()));

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        });
    }

    @Test
    void patchDocument_ServiceException() {
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand, getHttpHeaders());
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
        )).thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.BAD_GATEWAY, "403", new HttpHeaders(), new byte[1],
                                                              Charset.defaultCharset()));

        Assertions.assertThrows(ServiceException.class, () -> {
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
        Assertions.assertThrows(BadRequestException.class, () -> {
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID), updateDocumentCommand);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_HappyPath() {
        Boolean permanent = true;
        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=%s", documentURL, MATCHED_DOCUMENT_ID, permanent);

        ResponseEntity responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            Object.class
        )).thenReturn(new ResponseEntity<>(responseEntity, HttpStatus.NO_CONTENT));

        responseEntity = sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        assertEquals(HttpStatus.NO_CONTENT,responseEntity.getStatusCode());
    }



    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_NotFoundException() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            Object.class
        )).thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.NOT_FOUND, "404", new HttpHeaders(), new byte[1],
                                                              Charset.defaultCharset()));

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_ForbiddenException() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            Object.class
        )).thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.FORBIDDEN, "404", new HttpHeaders(), new byte[1],
                                                              Charset.defaultCharset()));

        Assertions.assertThrows(ForbiddenException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_BadRequestException() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            Object.class
        )).thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.BAD_REQUEST, "404", new HttpHeaders(), new byte[1],
                                                              Charset.defaultCharset()));

        Assertions.assertThrows(BadRequestException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_ServiceException() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            Object.class
        )).thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.BAD_GATEWAY, "404", new HttpHeaders(), new byte[1],
                                                              Charset.defaultCharset()));

        Assertions.assertThrows(ServiceException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_ResourceNotFound() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL, MATCHED_DOCUMENT_ID);

        ResponseEntity responseEntity = new ResponseEntity<>(HttpStatus.ACCEPTED);
        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            Object.class
        )).thenReturn(new ResponseEntity<>(responseEntity, HttpStatus.NOT_FOUND));

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkUserPermissionTest2() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","!!VCB12");
        storedDocumentHalResource.setMetadata(myMap);

        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class))).thenReturn(Optional.of(doc));

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);

        Assertions.assertThrows(BadRequestException.class, () -> {
            sut.checkUserPermission(responseEntity, UUID.fromString(MATCHED_DOCUMENT_ID), Permission.CREATE);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkUserPermissionTest3() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234567890123456");
        storedDocumentHalResource.setMetadata(myMap);

        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class))).thenReturn(Optional.of(doc));

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);

        Boolean result = sut.checkUserPermission(responseEntity, UUID.fromString(MATCHED_DOCUMENT_ID), Permission.CREATE);
        assertEquals(false, result);
    }

}
