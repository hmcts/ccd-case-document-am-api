package uk.gov.hmcts.reform.ccd.document.am.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class WelcomeTest {
    private transient WelcomeController caseDocumentAmController = new WelcomeController();

    @Test
    public void shouldReturnWelcomeMessage() {
        ResponseEntity<String> caseDocumentControllerResponse = caseDocumentAmController.welcome();
        assertNotNull(caseDocumentControllerResponse, "No Response from WelcomeController");
        assertEquals(HttpStatus.OK, caseDocumentControllerResponse.getStatusCode(), "Status code is NOT OK");
        assertEquals("Welcome to CCD Case Document AM Controller",
                     caseDocumentControllerResponse.getBody(),
            "Response body does not have expected value");
    }
}
