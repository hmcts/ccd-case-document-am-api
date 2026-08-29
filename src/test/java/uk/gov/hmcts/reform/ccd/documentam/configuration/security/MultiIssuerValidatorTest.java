package uk.gov.hmcts.reform.ccd.documentam.configuration.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiIssuerValidatorTest {

    private static final String OIDC_ISSUER = "https://idam-web-public.aat.platform.hmcts.net/o";
    private static final String HMCTS_ACCESS_ISSUER = "https://hmcts-access.aat.platform.hmcts.net/o";
    private static final String IDAM_API_BASE_ISSUER = "https://idam-api.aat.platform.hmcts.net";
    private static final String IDAM_API_ISSUER = "https://idam-api.aat.platform.hmcts.net/o";
    private static final String FORGEROCK_ISSUER =
        "https://forgerock-am.service.core-compute-idam-aat.internal:8443/openam/oauth2/hmcts";
    private static final String OIDC_ISSUER_WITH_TRAILING_SLASH = OIDC_ISSUER + "/";

    private final MultiIssuerValidator validator = new MultiIssuerValidator(
        List.of(OIDC_ISSUER, HMCTS_ACCESS_ISSUER, FORGEROCK_ISSUER, IDAM_API_BASE_ISSUER, IDAM_API_ISSUER)
    );

    private final MultiIssuerValidator validatorWithTrailingSlashConfig = new MultiIssuerValidator(
        List.of(OIDC_ISSUER_WITH_TRAILING_SLASH)
    );

    @Test
    void shouldAcceptOidcIssuer() {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithIssuer(OIDC_ISSUER));

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldAcceptForgerockIssuer() {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithIssuer(FORGEROCK_ISSUER));

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldAcceptHmctsAccessIssuer() {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithIssuer(HMCTS_ACCESS_ISSUER));

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldAcceptIdamApiBaseIssuer() {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithIssuer(IDAM_API_BASE_ISSUER));

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldAcceptIdamApiIssuer() {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithIssuer(IDAM_API_ISSUER));

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldAcceptIssuerWithTrailingSlashInToken() {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithIssuer(OIDC_ISSUER_WITH_TRAILING_SLASH));

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldAcceptIssuerWithoutTrailingSlashWhenConfiguredWithSlash() {
        OAuth2TokenValidatorResult result = validatorWithTrailingSlashConfig.validate(jwtWithIssuer(OIDC_ISSUER));

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldRejectUnknownIssuer() {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithIssuer("https://unknown-issuer.example.com"));

        assertTrue(result.hasErrors());
    }

    @Test
    void shouldRejectMissingIssuer() {
        OAuth2TokenValidatorResult result = validator.validate(
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .build()
        );

        assertTrue(result.hasErrors());
    }

    private Jwt jwtWithIssuer(String issuer) {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer(issuer)
            .build();
    }
}
