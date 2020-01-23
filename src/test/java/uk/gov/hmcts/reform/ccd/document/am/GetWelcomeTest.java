package uk.gov.hmcts.reform.ccd.document.am;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.controllers.CaseDocumentAmController;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class GetWelcomeTest {
    private final CaseDocumentAmController caseDocumentAmController = new CaseDocumentAmController();

    @Test
    public void shouldReturnWelcomeMessage() {
        ResponseEntity<String> caseDocumentControllerResponse = caseDocumentAmController.welcome();
        assertNotNull(caseDocumentControllerResponse, "No Response from CaseDocumentAmController");
        assertEquals(HttpStatus.OK, caseDocumentControllerResponse.getStatusCode(), "Status code is NOT OK");
        assertEquals(caseDocumentControllerResponse.getBody(),
            "Welcome to CCD Case Document AM Controller", "Response body does not have expected value");
    }

    @Test
    public void shoudReturnCaseNumbers() {
        ResponseEntity<List<String>> responseEntity = caseDocumentAmController.getCases();
        assertNotNull(responseEntity,"Response Enttity should not be null");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "Response Enttity should not be null");
        assertEquals(5, responseEntity.getBody().size(), "Size should be 5");
        assertEquals("C101", responseEntity.getBody().get(0), "Case Id check");
    }
}
