package uk.gov.hmcts.reform.ccd.document.am.controllers.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class S2SAuthentication {

    @RequestMapping(value = "/testS2SAuthorization", method = RequestMethod.GET)
    public ResponseEntity<String> testS2SAuthorization() {

        return ok("S2S Authentication is successful !!");
    }


}


