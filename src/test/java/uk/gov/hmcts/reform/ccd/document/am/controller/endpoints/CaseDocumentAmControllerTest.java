package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


public class CaseDocumentAmControllerTest {
    @Autowired
    transient CaseDocumentAmController caseDocumentAmControllerTest;

    transient StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();

    @Before
    public void setUp() {
    }

    @Test
    public void shouldGetMetaDataWhenResponseIsValid() {
//        final UUID documentId = UUID.randomUUID();
//        ResponseEntity<StoredDocumentHalResource> response = caseDocumentAmControllerTest.getDocumentbyDocumentId(documentId);
//        assertNotNull(response, "Valid Response from /api/cases/documents/{documentId} in CaseDocumentAmController");
//        assertNotNull(response.getBody());
//        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK");
    }

    @Test
    public void shouldNotGetMetaDataWhenResponseIsInValid() {
//        final UUID documentId = UUID.randomUUID();
//        ResponseEntity<StoredDocumentHalResource> response = caseDocumentAmControllerTest.getDocumentbyDocumentId(documentId);
//        // assert statements
//        assertNotNull(response);
//        assertNotNull(response.getBody());
//        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK");
    }

    @Test
    public void shouldGetCaseIdWhenMetaDataContainsValidCaseId() {
//        String caseId = tester.extractDocumentMetadata(storedDocumentHalResource);
//        assertNotNull(caseId);
    }

    @Test
    public void shouldNotGetCaseIdWhenMetaDataDoesNotContainsValidCaseId() {
//        String caseId = tester.extractDocumentMetadata(storedDocumentHalResource);
//        assertNull(caseId);

    }
}
