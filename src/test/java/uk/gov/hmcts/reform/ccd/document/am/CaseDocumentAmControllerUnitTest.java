package uk.gov.hmcts.reform.ccd.document.am;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.controllers.CaseDocumentAmController;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;




public class CaseDocumentAmControllerUnitTest {
    private final CaseDocumentAmController caseDocumentAmController = new CaseDocumentAmController();

    @Test
    public void shouldReturnWelcomeMessage() {
        ResponseEntity<String> caseDocumentControllerResponse = caseDocumentAmController.welcome();
        assertNotNull(caseDocumentControllerResponse);
        assertEquals(HttpStatus.OK, caseDocumentControllerResponse.getStatusCode());
        assertEquals(caseDocumentControllerResponse.getBody(),
            "Welcome to CCD Case Document AM Controller");
    }

    @Test
    public void shoudReturnCaseNumbers() {
        ResponseEntity<List<String>> responseEntity = caseDocumentAmController.getCases();
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(5, responseEntity.getBody().size());
        assertEquals("C101", responseEntity.getBody().get(0));
    }
}
