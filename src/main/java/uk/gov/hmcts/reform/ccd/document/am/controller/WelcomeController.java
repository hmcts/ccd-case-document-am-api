package uk.gov.hmcts.reform.ccd.document.am.controller;

import java.util.UUID;

import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

/**
 * Default endpoints per application.
 */

@RestController
public class WelcomeController {

    @GetMapping(value = {"/", "/health"})
    public Health healthCheck() {
        return Health.up().build();
    }

    @GetMapping(value = "/swagger")
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
