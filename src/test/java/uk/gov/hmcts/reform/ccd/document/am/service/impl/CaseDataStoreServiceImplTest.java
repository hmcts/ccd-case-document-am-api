package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class CaseDataStoreServiceImplTest {

    private static final String CASE_ID = "1582550122096256";
    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";

    private transient RestTemplate restTemplate = mock(RestTemplate.class);
    private AuthTokenGenerator authTokenGenerator = mock(AuthTokenGenerator.class);
    private transient SecurityUtils securityUtils = new SecurityUtils(authTokenGenerator);

    private CaseDataStoreServiceImpl sut = new CaseDataStoreServiceImpl(restTemplate,securityUtils);

    Optional<CaseDocumentMetadata> caseDocumentMetadataResponse;

    @Value("${caseDataStoreUrl}")
    String caseDataStoreUrl;

    private String authorization = "auth";

    @Test
    void getCaseDocumentMetadata_HappyPath() {

        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        Document doc;
        doc = Document.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();

        CaseDocumentMetadata caseDocMetaData = new CaseDocumentMetadata();
        caseDocMetaData.setCaseId(CASE_ID);
        caseDocMetaData.setDocument(doc);

        Map<String,CaseDocumentMetadata> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("documentMetadata",caseDocMetaData);
        HttpHeaders headers = prepareRequestForUpload(authorization);

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenReturn(new ResponseEntity<>(linkedHashMap, HttpStatus.OK));

        caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(CASE_ID, UUID.fromString(MATCHED_DOCUMENT_ID), authorization);
        assertNotNull(caseDocumentMetadataResponse);
        assertEquals(CASE_ID,caseDocumentMetadataResponse.get().getCaseId());
        assertEquals(MATCHED_DOCUMENT_ID,caseDocumentMetadataResponse.get().getDocument().getId());
    }

    @Test
    void getCaseDocumentMetadata_NoDocumentThrowsException() {

        HttpHeaders headers = prepareRequestForUpload(authorization);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        CaseDocumentMetadata caseDocMetaData = new CaseDocumentMetadata();
        caseDocMetaData.setCaseId(CASE_ID);

        Map<String,CaseDocumentMetadata> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("documentMetadata",caseDocMetaData);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenReturn(new ResponseEntity<>(linkedHashMap, HttpStatus.OK));

        Assertions.assertThrows(ForbiddenException.class, () -> {
            caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(
                CASE_ID,
                UUID.fromString(MATCHED_DOCUMENT_ID),
                authorization
            );
        });
    }

    @Test
    void getCaseDocumentMetadata_HttpClientErrorNotFound() {

        HttpHeaders headers = prepareRequestForUpload(authorization);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.NOT_FOUND, "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        Assertions.assertThrows(ForbiddenException.class, () -> {
            caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(CASE_ID, UUID.fromString(MATCHED_DOCUMENT_ID), authorization);
        });
    }

    @Test
    void getCaseDocumentMetadata_HttpClientErrorForbiddenException() {

        HttpHeaders headers = prepareRequestForUpload(authorization);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.FORBIDDEN, "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        Assertions.assertThrows(ForbiddenException.class, () -> {
            caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(CASE_ID, UUID.fromString(MATCHED_DOCUMENT_ID), authorization);
        });
    }

    @Test
    void getCaseDocumentMetadata_HttpClientErrorBadRequestException() {

        HttpHeaders headers = prepareRequestForUpload(authorization);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.BAD_REQUEST, "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        Assertions.assertThrows(BadRequestException.class, () -> {
            caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(CASE_ID, UUID.fromString(MATCHED_DOCUMENT_ID), authorization);
        });
    }

    @Test
    void getCaseDocumentMetadata_ServiceException() {

        HttpHeaders headers = prepareRequestForUpload(authorization);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.UNAUTHORIZED, "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        Assertions.assertThrows(ServiceException.class, () -> {
            caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(CASE_ID, UUID.fromString(MATCHED_DOCUMENT_ID), authorization);
        });
    }

    private HttpHeaders prepareRequestForUpload(String authorization) {

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(securityUtils.authorizationHeaders());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("experimental", "true");
        headers.set("Authorization", authorization);
        return headers;
    }
}
