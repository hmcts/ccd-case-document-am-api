package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class S2SAuthentication {

    private static final Logger LOG = LoggerFactory.getLogger(S2SAuthentication.class);
    private transient AuthTokenGenerator authTokenGenerator;

    @RequestMapping(value = "/testS2SAuthorization", method = RequestMethod.GET)
    public ResponseEntity<String> testS2SAuthorization() {

        LOG.info("Token Generation ", authTokenGenerator.generate());

        return ok("S2S Authentication is successful !!"+authTokenGenerator.generate());
    }
}


