package uk.gov.hmcts.reform.ccd.document.am;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.controllers.CaseDocumentAMController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class GetWelcomeTest {
    private final CaseDocumentAMController caseDocumentAMController = new CaseDocumentAMController();

    @Test
    public void shouldReturnWelcomeMessage() {
        ResponseEntity<String> caseDocumentControllerResponse = caseDocumentAMController.welcome();
        assertNotNull(caseDocumentControllerResponse);
        assertEquals(HttpStatus.OK, caseDocumentControllerResponse.getStatusCode());

    }
}
