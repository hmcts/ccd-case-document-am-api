package uk.gov.hmcts.reform.ccd.documentam.configuration;

import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigurationIT extends BaseTest {

    private static final String UNEXPECTED_ISSUER = "http://unexpected-issuer/o";

    @Autowired
    private JwtDecoder jwtDecoder;

    @Value("${oidc.issuer}")
    private String enforcedIssuer;

    @Test
    void shouldDecodeJwtWhenTokenIssMatchesConfiguredIssuer() throws JOSEException {
        Jwt jwt = assertDoesNotThrow(() -> jwtDecoder.decode(generateAuthToken(AUTH_TOKEN_TTL, enforcedIssuer)));

        assertThat(jwt.getIssuer().toString()).isEqualTo(enforcedIssuer);
    }

    @Test
    void shouldRejectJwtWhenTokenIssIsUnexpected() throws JOSEException {
        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> jwtDecoder.decode(generateAuthToken(AUTH_TOKEN_TTL, UNEXPECTED_ISSUER))
        );

        assertThat(exception.getMessage()).contains("iss");
    }
}
