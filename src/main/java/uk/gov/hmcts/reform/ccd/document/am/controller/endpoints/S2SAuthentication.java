package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class S2SAuthentication {

    @GetMapping(value = "/testS2SAuthorization")
    public ResponseEntity<String> testS2SAuthorization() {
        //Only for Testing to Confirm we are getting userid and roles basaed on user token
        return ok("S2S Authentication is successful !!");
    }
}


