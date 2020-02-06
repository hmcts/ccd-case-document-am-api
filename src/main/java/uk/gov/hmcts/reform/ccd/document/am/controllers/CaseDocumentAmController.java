package uk.gov.hmcts.reform.ccd.document.am.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.document.am.controllers.advice.exception.UnauthorizedException;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Default endpoints per application.
 */
@RestController
public class CaseDocumentAmController {

    private static final Logger logger = LoggerFactory.getLogger(CaseDocumentAmController.class);

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

    @GetMapping("/cases")
    public ResponseEntity<List<String>> getCases() {
        throw new UnauthorizedException("This is an UnauthorizedException");
    }
}
