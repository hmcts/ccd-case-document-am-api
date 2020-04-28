package uk.gov.hmcts.reform.ccd.document.am.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.CaseNotFoundException;

import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

import java.util.UUID;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Default endpoints per application.
 */

@RestController
public class WelcomeController {

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

    @GetMapping("/exception/{type}")
    public ResponseEntity<String> getException(@PathVariable String type) {
        if (type.equals("requiredFieldMissingException")) {
            throw new RequiredFieldMissingException("Required field is missing");
        } else if (type.equals("invalidRequest")) {
            throw new InvalidRequest("Invalid Request");
        } else if (type.equals("resourceNotFoundException")) {
            throw new ResourceNotFoundException("Resource Not Found Exception");
        } else if (type.equals("httpMessageConversionException")) {
            throw new HttpMessageConversionException("Http Message Conversion Exception");
        } else if (type.equals("badRequestException")) {
            throw new BadRequestException("Bad Request Exception");
        } else if (type.equals("caseNotFoundException")) {
            throw new CaseNotFoundException("Case Not Found Exception");
        }

        return null;
    }
}
