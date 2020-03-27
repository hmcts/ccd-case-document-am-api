package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUpdate;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.util.ApplicationUtils;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PATCH;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.BINARY;
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
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.USER_ROLES;

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

@RunWith(MockitoJUnitRunner.class)
class DocumentManagementServiceImplTest {

    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";
    private static final String UNMATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9d";

    private transient String serviceAuthorization = "auth";
    private static final String CASE_ID = "1582550122096256";
    private static final String BEFTA_CASETYPE_2 =  "BEFTA_CASETYPE_2";
    private static final String BEFTA_JURISDICTION_2 =  "BEFTA_JURISDICTION_2";
    private static final String USER_ID =  "userId";

    private AuthTokenGenerator authTokenGenerator = mock(AuthTokenGenerator.class);
    private RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);
    private SecurityUtils securityUtils = new SecurityUtils(authTokenGenerator);
    private CaseDataStoreService caseDataStoreServiceMock = mock(CaseDataStoreService.class);

    private HttpEntity<?> requestEntityGlobal  = new HttpEntity<>(securityUtils.authorizationHeaders());
    private UUID matchedDocUUID = UUID.fromString(MATCHED_DOCUMENT_ID);

    @InjectMocks
    private DocumentManagementServiceImpl sut = new DocumentManagementServiceImpl(restTemplateMock, securityUtils,
                                                                                  caseDataStoreServiceMock);

    @Value("${documentStoreUrl}")
    String documentURL = "http://localhost:4506";

    @Value("${documentTTL}")
    protected String documentTTL = "600000"; //TODO this @Value annotation is not working so I have to set the value to test.

    @Test
    void documentMetadataInstantiation() {
        assertNotNull(sut);
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
        myMap.put("caseId","1234qwer1234qwer");
        storedDocumentHalResource.setMetadata(myMap);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        Document doc;
        doc = Document.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        CaseDocumentMetadata cdm = CaseDocumentMetadata.builder().caseId("1234qwer1234qwer").document(doc).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class),anyString())).thenReturn(Optional.of(cdm));

        Boolean result = sut.checkUserPermission(responseEntity, matchedDocUUID,"auth", Permission.READ);
        assertEquals(Boolean.TRUE, result);

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    @Test
    void checkUserPermission_Throws_CaseNotFoundException() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234qwer1234qwe5");
        storedDocumentHalResource.setMetadata(myMap);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class),anyString()))
            .thenReturn(null);

        Assertions.assertThrows(Exception.class, () -> {
            sut.checkUserPermission(responseEntity, matchedDocUUID,"auth", Permission.READ);
        });

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    @Test
    void checkUserPermission_Throws_InvalidCaseId_BadRequestException() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234qwer");
        storedDocumentHalResource.setMetadata(myMap);
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(responseEntity.getStatusCode(),HttpStatus.OK);

        Assertions.assertThrows(BadRequestException.class, () -> {
            sut.checkUserPermission(responseEntity, matchedDocUUID,"auth", Permission.READ);
        });

        verifyRestExchangeOnStoredDoc();
    }

    @Test
    void checkUserPermission_ReturnsFalse_Scenario1() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234qwer1234qwer");
        storedDocumentHalResource.setMetadata(myMap);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(responseEntity.getStatusCode(),HttpStatus.OK);
        List<Permission> permissionsList = new ArrayList<>();
        Document doc;
        doc = Document.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        CaseDocumentMetadata cdm = CaseDocumentMetadata.builder().caseId("1234qwer1234qwer").document(doc).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class),anyString()))
            .thenReturn(Optional.ofNullable(cdm));

        Boolean result = sut.checkUserPermission(responseEntity, matchedDocUUID,"auth", Permission.READ);
        assertEquals(Boolean.FALSE, result);

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    @Test
    void checkUserPermission_ReturnsFalse_Scenario2() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234qwer1234qwer");
        storedDocumentHalResource.setMetadata(myMap);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(responseEntity.getStatusCode(),HttpStatus.OK);
        List<Permission> permissionsList = new ArrayList<>();
        Document doc;
        doc = Document.builder().id("40000a2b-00ce-00eb-0068-2d00a700be9c").permissions(permissionsList).build();
        CaseDocumentMetadata cdm = CaseDocumentMetadata.builder().caseId("1234qwer1234qwer").document(doc).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class),anyString()))
            .thenReturn(Optional.ofNullable(cdm));

        Boolean result = sut.checkUserPermission(responseEntity, matchedDocUUID,"auth", Permission.READ);
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
            .getCaseDocumentMetadata(anyString(),any(UUID.class),anyString());
    }

    @Test
    void patchDocumentMetadata_HappyPath() {
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.UPDATE);
        Document doc = Document.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList)
            .hashToken(ApplicationUtils.generateHashCode(MATCHED_DOCUMENT_ID.concat(BEFTA_JURISDICTION_2).concat(BEFTA_CASETYPE_2))).build();
        List<Document> documentList = new ArrayList<>();
        documentList.add(doc);


        Map<String, String> myMetadata = new HashMap<>();
        myMetadata.put("caseId",CASE_ID);
        myMetadata.put("jurisdictionId", BEFTA_JURISDICTION_2);
        myMetadata.put("caseTypeId", BEFTA_CASETYPE_2);
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        storedDocumentHalResource.setMetadata(myMetadata);
        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);

        DocumentUpdate documentUpdate = new DocumentUpdate();
        documentUpdate.setDocumentId(UUID.fromString(MATCHED_DOCUMENT_ID));
        documentUpdate.setMetadata(myMetadata);
        LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add("documents", documentUpdate);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(SERVICE_AUTHORIZATION, serviceAuthorization);
        headers.set(USERID, USER_ID);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
        String documentUrl = String.format("%s/documents", documentURL);

        Mockito.when(restTemplateMock.exchange(
            documentUrl,
            HttpMethod.PATCH,requestEntity,
            Void.class))
            .thenReturn(new ResponseEntity<>(HttpStatus.ACCEPTED));

        DocumentMetadata documentMetadata = DocumentMetadata.builder()
            .caseId(CASE_ID)
            .caseTypeId(BEFTA_CASETYPE_2)
            .jurisdictionId(BEFTA_JURISDICTION_2)
            .documents(documentList)
            .build();

        Boolean response = sut.patchDocumentMetadata(documentMetadata,"auth",USER_ID);
        assertEquals(Boolean.TRUE, response);

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

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ResponseEntity<Object> responseEntity = sut.uploadDocuments(files,"classification", roles,
                                                                    serviceAuthorization, BEFTA_CASETYPE_2, BEFTA_JURISDICTION_2,USER_ID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadDocuments_NoLinksExceptionThrow() {

        HashMap<String, String> binaryHash = new HashMap<>();
        HashMap<String, String> selfHash = new HashMap<>();
        selfHash.put(HREF, "http://localhost:4455/cases/documents/35471d43-0dad-42c1-b05a-4821028f50a2");
        binaryHash.put(HREF, "http://localhost:4455/cases/documents/35471d43-0dad-42c1-b05a-4821028f50a2/binary");

        LinkedHashMap<String, Object> linksLinkedHashMap = new LinkedHashMap<>();
        LinkedHashMap<String, Object> binarySelfLinkedHashMap = new LinkedHashMap<>();

        ArrayList arrayList = new ArrayList();
        arrayList.add(linksLinkedHashMap);

        LinkedHashMap<String, Object> documentsLinkedHashMap = new LinkedHashMap<>();
        documentsLinkedHashMap.put(DOCUMENTS,arrayList);

        LinkedHashMap<String, Object> embeddedLinkedHashMap = new LinkedHashMap<>();
        embeddedLinkedHashMap.put(EMBEDDED,documentsLinkedHashMap);

        Mockito.when(restTemplateMock.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(embeddedLinkedHashMap, HttpStatus.OK));

        List<MultipartFile> files = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        roles.add("Role");

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Assertions.assertThrows(ResponseFormatException.class, () -> {
            ResponseEntity<Object> responseEntity = sut.uploadDocuments(files,
                                                                        "classification",
                                                                        roles,
                                                                        serviceAuthorization,
                                                                        BEFTA_CASETYPE_2,
                                                                        BEFTA_JURISDICTION_2,
                                                                        USER_ID
            );
        });
    }

    private String getEffectiveTTL() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        return format.format(new Timestamp(new Date().getTime() + Long.parseLong(documentTTL)));
    }

    @Test
    void patchDocument_HappyPath() {
        List<String> roles = new ArrayList<>();
        roles.add("Role");
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand, getHttpHeaders(USER_ID, USER_ROLES));
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
        )).thenReturn(new ResponseEntity<>(storedDocumentHalResource, HttpStatus.OK));

        ResponseEntity responseEntity = sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID),updateDocumentCommand,USER_ID,USER_ROLES);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
    }

    private HttpHeaders getHttpHeaders(String userId, String userRoles) {
        HttpHeaders headers = securityUtils.authorizationHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(USERID, userId);
        headers.add(USER_ROLES, userRoles);
        return headers;
    }

    @Test
    void patchDocument_ResourceNotFound() {
        List<String> roles = new ArrayList<>();
        roles.add("Role");
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand, getHttpHeaders(USER_ID, USER_ROLES));
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
        )).thenReturn(new ResponseEntity<>(storedDocumentHalResource, HttpStatus.NOT_FOUND));

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID),updateDocumentCommand,USER_ID,USER_ROLES);
        });
    }

    @Test
    void patchDocument_HttpClientErrorException() {
        List<String> roles = new ArrayList<>();
        roles.add("Role");
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand, getHttpHeaders(USER_ID, USER_ROLES));
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
        )).thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.NOT_FOUND, "404", new HttpHeaders(), new byte[1],
                                                              Charset.defaultCharset()));

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID),updateDocumentCommand,USER_ID,USER_ROLES);
        });
    }

    @Test
    void patchDocument_ServiceException() {
        List<String> roles = new ArrayList<>();
        roles.add("Role");
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        String effectiveTTL = getEffectiveTTL();
        updateDocumentCommand.setTtl(effectiveTTL);
        final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(updateDocumentCommand, getHttpHeaders(USER_ID, USER_ROLES));
        String patchTTLUrl = String.format("%s/documents/%s", documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            patchTTLUrl,
            PATCH,
            requestEntity,
            StoredDocumentHalResource.class
        )).thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.FORBIDDEN, "404", new HttpHeaders(), new byte[1],
                                                              Charset.defaultCharset()));

        Assertions.assertThrows(ServiceException.class, () -> {
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID),updateDocumentCommand,USER_ID,USER_ROLES);
        });
    }

    @Test
    void patchDocument_BadRequestTTL() {
        List<String> roles = new ArrayList<>();
        roles.add("Role");
        UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();
        updateDocumentCommand.setTtl("600000");
        Assertions.assertThrows(BadRequestException.class, () -> {
            sut.patchDocument(UUID.fromString(MATCHED_DOCUMENT_ID),updateDocumentCommand, USER_ID,"Role");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_HappyPath() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders(USER_ID, USER_ROLES));
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL, MATCHED_DOCUMENT_ID);

        ResponseEntity responseEntity = new ResponseEntity<>(HttpStatus.ACCEPTED);
        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            ResponseEntity.class
        )).thenReturn(new ResponseEntity<>(responseEntity, HttpStatus.NO_CONTENT));

        responseEntity = sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID),USER_ID,USER_ROLES,permanent);
        assertEquals(HttpStatus.NO_CONTENT,responseEntity.getStatusCode());
    }



    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_HttpClientErrorException() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders(USER_ID, USER_ROLES));
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            ResponseEntity.class
        )).thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.NOT_FOUND, "404", new HttpHeaders(), new byte[1],
                                                              Charset.defaultCharset()));

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), USER_ID, USER_ROLES, permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_ServiceException() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders(USER_ID, USER_ROLES));
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL, MATCHED_DOCUMENT_ID);

        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            ResponseEntity.class
        )).thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.FORBIDDEN, "404", new HttpHeaders(), new byte[1],
                                                              Charset.defaultCharset()));

        Assertions.assertThrows(ServiceException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), USER_ID, USER_ROLES, permanent);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteDocument_ResourceNotFound() {
        Boolean permanent = true;

        HttpEntity requestEntity = new HttpEntity(getHttpHeaders(USER_ID, USER_ROLES));
        String documentDeleteUrl = String.format("%s/documents/%s?permanent=" + permanent, documentURL, MATCHED_DOCUMENT_ID);

        ResponseEntity responseEntity = new ResponseEntity<>(HttpStatus.ACCEPTED);
        when(restTemplateMock.exchange(
            documentDeleteUrl,
            DELETE,
            requestEntity,
            ResponseEntity.class
        )).thenReturn(new ResponseEntity<>(responseEntity, HttpStatus.NOT_FOUND));

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            sut.deleteDocument(UUID.fromString(MATCHED_DOCUMENT_ID), USER_ID, USER_ROLES, permanent);
        });
    }
}
