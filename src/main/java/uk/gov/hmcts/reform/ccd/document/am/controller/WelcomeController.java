package uk.gov.hmcts.reform.ccd.document.am.controller;

import static org.springframework.http.ResponseEntity.ok;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

/**
 * Default endpoints per application.
 */

@RestController
public class WelcomeController {

    private static final Logger logger = LoggerFactory.getLogger(WelcomeController.class);

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping("/")
    public ResponseEntity<String> welcome() {
        return ok("Welcome to CCD Case Document AM Controller");
    }

    @RequestMapping("/swagger")
    public String index() {
        return "redirect:swagger-ui.html";
    }

    @GetMapping(value = "/api/cases/documents/{documentId}")
    public ResponseEntity<StoredDocumentHalResource> getMetaData(@PathVariable UUID documentId) {
        return ResponseEntity
            .ok()
            .body(null);
    }

}
