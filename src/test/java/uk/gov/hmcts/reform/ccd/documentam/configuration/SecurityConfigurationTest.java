package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigurationTest {

    private static final String VALID_ISSUER = "http://fr-am:8080/openam/oauth2/hmcts";
    private static final String INVALID_ISSUER = "http://unexpected-issuer";

    @Test
    void shouldAcceptJwtFromConfiguredIssuer() {
        assertFalse(validator().validate(buildJwt(VALID_ISSUER, Instant.now().plusSeconds(300))).hasErrors());
    }

    @Test
    void shouldRejectJwtFromUnexpectedIssuer() {
        OAuth2TokenValidatorResult result =
            validator().validate(buildJwt(INVALID_ISSUER, Instant.now().plusSeconds(300)));

        assertTrue(result.hasErrors());
        assertThat(result.getErrors())
            .anySatisfy(error -> assertThat(error.getDescription()).contains("iss"));
    }

    @Test
    void shouldRejectExpiredJwtEvenWhenIssuerMatches() {
        assertTrue(validator().validate(buildJwt(VALID_ISSUER, Instant.now().minusSeconds(60))).hasErrors());
    }

    private OAuth2TokenValidator<Jwt> validator() {
        return new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtIssuerValidator(VALID_ISSUER)
        );
    }

    private Jwt buildJwt(String issuer, Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(60);
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuer(issuer)
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }
}
