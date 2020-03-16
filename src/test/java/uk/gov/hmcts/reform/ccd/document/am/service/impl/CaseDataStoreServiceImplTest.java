package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class CaseDataStoreServiceImplTest {

    private static final String CASE_ID = "1582550122096256";
    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";

    private transient RestTemplate restTemplate = mock(RestTemplate.class);
    private transient SecurityUtils securityUtils = mock(SecurityUtils.class);

    private CaseDataStoreServiceImpl sut = new CaseDataStoreServiceImpl(restTemplate,securityUtils);

    Optional<CaseDocumentMetadata> caseDocumentMetadata;

    private HttpEntity<?> requestEntityGlobal  = new HttpEntity<>(securityUtils.authorizationHeaders());

    @Value("${caseDataStoreUrl}")
    String caseDataStoreUrl;

    @Test
    void getCaseDocumentMetadataSuccess() {

        StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();
        Map<String, String> myMap = new HashMap<>();
        myMap.put("caseId","1234qwer1234qwer");
        storedDocumentHalResource.setMetadata(myMap);

        Mockito.when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        Mockito.when(restTemplate.exchange(caseDataStoreUrl
                         .concat("/cases/")
                         .concat(CASE_ID)
                         .concat("/documents/")
                         .concat(MATCHED_DOCUMENT_ID), HttpMethod.GET, requestEntityGlobal, StoredDocumentHalResource.class))
            .thenReturn(new ResponseEntity<>(storedDocumentHalResource, HttpStatus.OK));
        /*caseDocumentMetadata = sut.getCaseDocumentMetadata(CASE_ID, getUuid(MATCHED_DOCUMENT_ID),"auth");
        assertNotNull(caseDocumentMetadata);
        assertEquals(CASE_ID,caseDocumentMetadata.get().getCaseId());
        assertEquals(MATCHED_DOCUMENT_ID,caseDocumentMetadata.get().getDocument().getId());*/
    }

    private UUID getUuid(String id) {
        return UUID.fromString(id);
    }
}
