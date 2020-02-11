package uk.gov.hmcts.reform.ccd.document.am;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.controller.CaseDocumentAmController;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CaseDocumentAmControllerTest {

    transient CaseDocumentAmController tester = new CaseDocumentAmController();

    transient StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();

    @Before
    public void setUp() {
    }

    @Test
    public void shouldGetMetaDataWhenResponseIsValid() {
        final UUID documentId = UUID.randomUUID();
        ResponseEntity<StoredDocumentHalResource> response = tester.getMetaData(documentId);
        assertNotNull(response, "Valid Response from /api/cases/documents/{documentId} in CaseDocumentAmController");
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK");
    }

    @Test
    public void shouldNotGetMetaDataWhenResponseIsInValid() {
        final UUID documentId = UUID.randomUUID();
        ResponseEntity<StoredDocumentHalResource> response = tester.getMetaData(documentId);
        // assert statements
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK");
    }

    @Test
    public void shouldGetCaseIdWhenMetaDataContainsValidCaseId() {
        String caseId = tester.extractDocumentMetadata(storedDocumentHalResource);
        assertNotNull(caseId);
    }

    @Test
    public void shouldNotGetCaseIdWhenMetaDataDoesNotContainsValidCaseId() {
        String caseId = tester.extractDocumentMetadata(storedDocumentHalResource);
        assertNull(caseId);

    }
}
