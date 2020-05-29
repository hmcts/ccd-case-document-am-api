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
import uk.gov.hmcts.reform.ccd.document.am.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class CaseDataStoreServiceImplTest {

    private static final String CASE_ID = "1582550122096256";
    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";

    private transient RestTemplate restTemplate = mock(RestTemplate.class);
    private AuthTokenGenerator authTokenGenerator = mock(AuthTokenGenerator.class);
    private transient SecurityUtils securityUtils = new SecurityUtils(authTokenGenerator);

    private CaseDataStoreServiceImpl sut = new CaseDataStoreServiceImpl(restTemplate,securityUtils);

    Optional<DocumentPermissions> caseDocumentMetadataResponse;

    @Value("${caseDataStoreUrl}")
    String caseDataStoreUrl;

    @Test
    void getCaseDocumentMetadata_HappyPath() {

        List<Permission> permissionsList = new ArrayList<>();
        permissionsList.add(Permission.READ);
        DocumentPermissions doc;
        doc = DocumentPermissions.builder().id(MATCHED_DOCUMENT_ID).permissions(permissionsList).build();
        CaseDocumentMetadata caseDocumentMetadata = CaseDocumentMetadata.builder().caseId("1234567890123456")
            .documentPermissions(doc).build();

        Map<String,Object> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("documentMetadata",caseDocumentMetadata);
        HttpHeaders headers = prepareRequestForUpload();

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenReturn(new ResponseEntity<>(linkedHashMap, HttpStatus.OK));

        caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(CASE_ID, UUID.fromString(MATCHED_DOCUMENT_ID));
        assertNotNull(caseDocumentMetadataResponse);
        assertEquals(MATCHED_DOCUMENT_ID,caseDocumentMetadataResponse.get().getId());
    }


    @Test
    void getCaseDocumentMetadata_HttpClientErrorNotFound() {

        HttpHeaders headers = prepareRequestForUpload();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.NOT_FOUND, "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        Assertions.assertThrows(ForbiddenException.class, () -> {
            caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(CASE_ID, UUID.fromString(MATCHED_DOCUMENT_ID));
        });
    }

    @Test
    void getCaseDocumentMetadata_HttpClientErrorForbiddenException() {

        HttpHeaders headers = prepareRequestForUpload();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.FORBIDDEN, "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        Assertions.assertThrows(ForbiddenException.class, () -> {
            caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(CASE_ID, UUID.fromString(MATCHED_DOCUMENT_ID));
        });
    }

    @Test
    void getCaseDocumentMetadata_HttpClientErrorBadRequestException() {

        HttpHeaders headers = prepareRequestForUpload();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.BAD_REQUEST, "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        Assertions.assertThrows(BadRequestException.class, () -> {
            caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(CASE_ID, UUID.fromString(MATCHED_DOCUMENT_ID));
        });
    }

    @Test
    void getCaseDocumentMetadata_ServiceException() {

        HttpHeaders headers = prepareRequestForUpload();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, CASE_ID, MATCHED_DOCUMENT_ID);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.UNAUTHORIZED, "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        Assertions.assertThrows(ServiceException.class, () -> {
            caseDocumentMetadataResponse = sut.getCaseDocumentMetadata(CASE_ID, UUID.fromString(MATCHED_DOCUMENT_ID));
        });
    }

    private HttpHeaders prepareRequestForUpload() {

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(securityUtils.authorizationHeaders());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("experimental", "true");
        return headers;
    }
}
