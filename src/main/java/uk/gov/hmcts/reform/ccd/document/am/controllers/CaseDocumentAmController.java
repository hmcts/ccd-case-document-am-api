package uk.gov.hmcts.reform.ccd.document.am.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Default endpoints per application.
 */
@RestController
public class CaseDocumentAmController {

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


        return ok("S2S Authentication is successful !!");
    }

    @RequestMapping(value = "/cases/", method = RequestMethod.GET)
    public ResponseEntity<List<String>> getCases() {
        List<String> cases = Arrays.asList("C101", "C102", "C103", "C104", "C105");
        return new ResponseEntity<List<String>>(cases, HttpStatus.OK);
    }
}
