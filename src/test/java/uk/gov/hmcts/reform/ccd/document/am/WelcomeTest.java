package uk.gov.hmcts.reform.ccd.document.am;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.controller.CaseDocumentAmController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class WelcomeTest {
    private transient CaseDocumentAmController caseDocumentAmController = new CaseDocumentAmController();

    @Test
    public void shouldReturnWelcomeMessage() {
        ResponseEntity<String> caseDocumentControllerResponse = caseDocumentAmController.welcome();
        assertNotNull(caseDocumentControllerResponse, "No Response from CaseDocumentAmController");
        assertEquals(HttpStatus.OK, caseDocumentControllerResponse.getStatusCode(), "Status code is NOT OK");
        assertEquals(caseDocumentControllerResponse.getBody(),
            "Welcome to CCD Case Document AM Controller",
            "Response body does not have expected value");
    }
}
