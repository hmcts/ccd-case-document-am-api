package uk.gov.hmcts.reform.ccd.document.am.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class WelcomeTest {
    private transient WelcomeController caseDocumentAmController = new WelcomeController();

    @Test
    public void shouldGetReturnIndex() {
        String response = caseDocumentAmController.index();
        assertNotNull(response, "Valid Response from WelcomeController");
        assertEquals("redirect:swagger-ui.html", response, "Response Value");
    }

    @Test
    public void shouldGetMetaData() {
        UUID doc = UUID.fromString("41334a2b-79ce-44eb-9168-2d49a744be9c");
        ResponseEntity<StoredDocumentHalResource>  response = caseDocumentAmController.getMetaData(doc);
        assertNotNull(response, "Valid Response from WelcomeController");
    }

}
