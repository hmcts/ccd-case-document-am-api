package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.configuration.ApplicationConfiguration;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class S2SAuthentication {

    private static final Logger LOG = LoggerFactory.getLogger(TestEndpoint.class);
    private transient AuthTokenGenerator authTokenGenerator;
    private transient ApplicationConfiguration applicationConfiguration;

    public S2SAuthentication(AuthTokenGenerator authTokenGenerator, ApplicationConfiguration applicationConfiguration) {
        this.authTokenGenerator = authTokenGenerator;
        this.applicationConfiguration = applicationConfiguration;
    }

    @RequestMapping(value = "/testS2SAuthorization", method = RequestMethod.GET)
    public ResponseEntity<String> testS2SAuthorization() {
        LOG.info("Scerect key at higher environment " + applicationConfiguration.getS2sSecret());
        LOG.info("One time password at PR environment " + applicationConfiguration.generate());
        LOG.info("Token Generation " + authTokenGenerator.generate());

        return ok("S2S Authentication is successful !!" + authTokenGenerator.generate());
    }
}


