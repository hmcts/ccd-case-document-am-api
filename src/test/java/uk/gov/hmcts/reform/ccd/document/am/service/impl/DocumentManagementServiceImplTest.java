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
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadDocumentsCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.service.common.ValidationService;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_LENGTH;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ORIGINAL_FILE_NAME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
class DocumentManagementServiceImplTest {

    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";

    private AuthTokenGenerator authTokenGenerator = mock(AuthTokenGenerator.class);
    private RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);
    private SecurityUtils securityUtils = new SecurityUtils(authTokenGenerator);
    private CaseDataStoreService caseDataStoreServiceMock = mock(CaseDataStoreService.class);
    private ValidationService validationService = mock(ValidationService.class);

    private HttpEntity<?> requestEntityGlobal  = new HttpEntity<>(securityUtils.authorizationHeaders());
    private UUID matchedDocUUID = UUID.fromString(MATCHED_DOCUMENT_ID);

    @InjectMocks
    private DocumentManagementServiceImpl sut = new DocumentManagementServiceImpl(restTemplateMock, securityUtils,
                                                                                  caseDataStoreServiceMock, validationService);

    @Value("${documentStoreUrl}")
    protected String documentURL;

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
            documentURL + "/" + MATCHED_DOCUMENT_ID,
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
            documentURL + "/" + MATCHED_DOCUMENT_ID,
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
        assertEquals(responseEntity.getStatusCode(),HttpStatus.OK);

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
            documentURL + "/" + MATCHED_DOCUMENT_ID + "/binary",
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
            documentURL + "/" + MATCHED_DOCUMENT_ID + "/binary",
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
            documentURL + "/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET,requestEntityGlobal,
            ByteArrayResource.class))
            .thenThrow(httpClientErrorException);
    }

    @Test
    void getDocumentBinaryContent_Throws_HttpClientErrorException_ServiceException() {
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/" + MATCHED_DOCUMENT_ID + "/binary",
            HttpMethod.GET,requestEntityGlobal,
            ByteArrayResource.class))
            .thenThrow(HttpClientErrorException.class);

        Assertions.assertThrows(ServiceException.class, () -> {
            sut.getDocumentBinaryContent(matchedDocUUID);
        });

        verifyRestExchangeByteArray();
    }

//    @Test
//    void uploadDocumentsContent() {
//        UploadDocumentsCommand uploadDocumentsCommand = mock(UploadDocumentsCommand.class);
//        StoredDocumentHalResourceCollection collection = sut.uploadDocumentsContent(uploadDocumentsCommand);
//        assertNull(collection);
//    }

    @Test
    void checkUserPermission_HappyPath() {
        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234qwer1234qwer");
        storedDocumentHalResource.setMetadata(myMap);

        mockitoWhenRestExchangeThenThrow(storedDocumentHalResource, HttpStatus.OK);
        ResponseEntity responseEntity = sut.getDocumentMetadata(matchedDocUUID);
        assertEquals(responseEntity.getStatusCode(),HttpStatus.OK);
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        Optional<Document> doc;
        doc = Optional.of(Document.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build());
        CaseDocumentMetadata cdm = CaseDocumentMetadata.builder().caseId("1234qwer1234qwer").document(doc).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class)))
            .thenReturn(Optional.ofNullable(cdm));

        Boolean result = sut.checkUserPermission(responseEntity, matchedDocUUID);
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
        assertEquals(responseEntity.getStatusCode(),HttpStatus.OK);
        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        Optional<Document> doc;
        doc = Optional.of(Document.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build());
        CaseDocumentMetadata cdm = CaseDocumentMetadata.builder().caseId("1234qwer1234qwer").document(doc).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class)))
            .thenReturn(null);

        Assertions.assertThrows(Exception.class, () -> {
            sut.checkUserPermission(responseEntity, matchedDocUUID);
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
            sut.checkUserPermission(responseEntity, matchedDocUUID);
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
        Optional<Document> doc;
        doc = Optional.of(Document.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build());
        CaseDocumentMetadata cdm = CaseDocumentMetadata.builder().caseId("1234qwer1234qwer").document(doc).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class)))
            .thenReturn(Optional.ofNullable(cdm));

        Boolean result = sut.checkUserPermission(responseEntity, matchedDocUUID);
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
        Optional<Document> doc;
        doc = Optional.of(Document.builder().id("40000a2b-00ce-00eb-0068-2d00a700be9c").permissions(permissionsList).build());
        CaseDocumentMetadata cdm = CaseDocumentMetadata.builder().caseId("1234qwer1234qwer").document(doc).build();
        Mockito.when(caseDataStoreServiceMock.getCaseDocumentMetadata(anyString(),any(UUID.class)))
            .thenReturn(Optional.ofNullable(cdm));

        Boolean result = sut.checkUserPermission(responseEntity, matchedDocUUID);
        assertEquals(Boolean.FALSE, result);

        verifyRestExchangeOnStoredDoc();
        verifyCaseDataServiceGetDocMetadata();
    }

    private void mockitoWhenRestExchangeThenThrow(StoredDocumentHalResource storedDocumentHalResource, HttpStatus httpStatus) {
        Mockito.when(restTemplateMock.exchange(
            documentURL + "/" + MATCHED_DOCUMENT_ID,
            HttpMethod.GET,requestEntityGlobal,
            StoredDocumentHalResource.class))
            .thenReturn(new ResponseEntity<>(storedDocumentHalResource,httpStatus));
    }

    private void verifyRestExchangeOnStoredDoc() {
        verify(restTemplateMock, times(1))
            .exchange(documentURL + "/" + MATCHED_DOCUMENT_ID,HttpMethod.GET,requestEntityGlobal, StoredDocumentHalResource.class);
    }

    private void verifyRestExchangeByteArray() {
        verify(restTemplateMock, times(1))
            .exchange(documentURL + "/" + MATCHED_DOCUMENT_ID + "/binary",HttpMethod.GET,requestEntityGlobal, ByteArrayResource.class);
    }

    private void verifyCaseDataServiceGetDocMetadata() {
        verify(caseDataStoreServiceMock, times(1))
            .getCaseDocumentMetadata(anyString(),any(UUID.class));
    }
}
