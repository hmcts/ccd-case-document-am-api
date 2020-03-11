package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import com.microsoft.applicationinsights.core.dependencies.google.api.Http;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.service.common.ValidationService;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockitoSession;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
class DocumentManagementServiceImplTest {

    private static final String CASE_ID = "1582550122096256";
    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";

    private AuthTokenGenerator authTokenGenerator = mock(AuthTokenGenerator.class);

    //@Mock
    private RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);

    private transient SecurityUtils securityUtils = new SecurityUtils(authTokenGenerator);
    private transient CaseDataStoreService caseDataStoreService;
    private transient ValidationService validationService;

    @InjectMocks
    private DocumentManagementServiceImpl sut = new DocumentManagementServiceImpl(restTemplateMock,securityUtils,caseDataStoreService,validationService);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        //restTemplateMock.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    }

    @Test
    void documentMetadataInstantiation() {
        assertNotNull(sut);
    }

    @Test
    void getDocumentMetadata() {
    StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        //storedDocumentHalResource.setCreatedBy("CONOR");
        //storedDocumentHalResource.setClassification(StoredDocumentHalResource.ClassificationEnum.PRIVATE);
        //storedDocumentHalResource.setLastModifiedBy("wow");
        //storedDocumentHalResource.setMimeType("wpw");
//        System.out.println(restTemplateMock.toString());
//        ResponseEntity<StoredDocumentHalResource> wow = new ResponseEntity<StoredDocumentHalResource>(storedDocumentHalResource,HttpStatus.OK);
        //when(wow.getBody()).thenReturn(storedDocumentHalResource);
        //restTemplateMock.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        HttpEntity<?> requestEntity = new HttpEntity<>(securityUtils.authorizationHeaders());
        //Mockito.when(restTemplateMock.exchange("url", HttpMethod.GET, HttpEntity.class, StoredDocumentHalResource.class).getBody()).thenReturn(new ResponseEntity<StoredDocumentHalResource>(storedDocumentHalResource,HttpStatus.OK));
        //Mockito.when(String.format(anyString(),anyString(),anyString())).thenReturn("http://document/41334a2b-79ce-44eb-9168-2d49a744be9");
        Mockito.when(restTemplateMock.exchange("null/41334a2b-79ce-44eb-9168-2d49a744be9c", HttpMethod.GET,requestEntity, StoredDocumentHalResource.class)).thenReturn(new ResponseEntity<StoredDocumentHalResource>(storedDocumentHalResource,HttpStatus.OK));
        //when(String.format(any(),any(),any()))
        sut.getDocumentMetadata(getUuid(MATCHED_DOCUMENT_ID));
    }

    @Test
    void get() {
        Assertions.assertThrows(NumberFormatException.class, () -> {
            Integer.parseInt("One");
        });
    }

    @Test
    void extractCaseIdFromMetadata() {
    }

    @Test
    void getDocumentBinaryContent() {
    }

    @Test
    void uploadDocumentsContent() {
    }

    @Test
    void checkUserPermission() {

    }

    private UUID getUuid(String id) {
        return UUID.fromString(id);
    }
}
