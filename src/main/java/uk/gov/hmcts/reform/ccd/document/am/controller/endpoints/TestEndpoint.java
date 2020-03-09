package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class TestEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(TestEndpoint.class);


    @RequestMapping(value = "/testPREnv", method = RequestMethod.GET)
    public ResponseEntity<String> testS2SAuthorization() {

        LOG.info("Token Generation");

        return ok("Test  at PR env !!");
    }
}


