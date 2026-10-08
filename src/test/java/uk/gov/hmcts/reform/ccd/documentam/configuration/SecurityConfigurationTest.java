package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationTest {

    private static final String ENFORCED_ISSUER = "http://fr-am:8080/openam/oauth2/hmcts";
    private static final String ALLOWED_MIGRATION_ISSUER = "http://allowed-issuer/o";
    private static final String SECOND_ALLOWED_MIGRATION_ISSUER = "http://second-allowed-issuer/o";
    private static final String UNEXPECTED_ISSUER = "http://unexpected-issuer";
    private static final Instant VALID_TOKEN_EXPIRES_AT = Instant.parse("2099-01-01T00:00:00Z");
    private static final Instant EXPIRED_TOKEN_EXPIRES_AT = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void shouldAcceptJwtFromConfiguredIssuerWhenAllowedIssuersAreEmpty() {
        assertThat(validate(ENFORCED_ISSUER, "", VALID_TOKEN_EXPIRES_AT).hasErrors()).isFalse();
    }

    @Test
    void shouldAcceptJwtFromConfiguredIssuerWhenAllowedIssuersAreConfigured() {
        assertThat(validate(ENFORCED_ISSUER, ALLOWED_MIGRATION_ISSUER, VALID_TOKEN_EXPIRES_AT).hasErrors())
            .isFalse();
    }

    @Test
    void shouldAcceptJwtFromAllowedMigrationIssuer() {
        assertThat(validate(ALLOWED_MIGRATION_ISSUER, ALLOWED_MIGRATION_ISSUER, VALID_TOKEN_EXPIRES_AT)
                       .hasErrors())
            .isFalse();
    }

    @Test
    void shouldAcceptJwtFromMultipleAllowedMigrationIssuers() {
        String allowedIssuers = ALLOWED_MIGRATION_ISSUER + ", " + SECOND_ALLOWED_MIGRATION_ISSUER;

        assertThat(validate(SECOND_ALLOWED_MIGRATION_ISSUER, allowedIssuers, VALID_TOKEN_EXPIRES_AT)
                       .hasErrors())
            .isFalse();
    }

    @Test
    void shouldRejectJwtFromUnexpectedIssuerWhenAllowedIssuersAreEmpty() {
        OAuth2TokenValidatorResult result =
            validate(UNEXPECTED_ISSUER, "", VALID_TOKEN_EXPIRES_AT);

        assertIssuerValidationError(result);
    }

    @Test
    void shouldRejectJwtFromUnexpectedIssuerWhenAllowedIssuersAreConfigured() {
        OAuth2TokenValidatorResult result =
            validate(UNEXPECTED_ISSUER, ALLOWED_MIGRATION_ISSUER, VALID_TOKEN_EXPIRES_AT);

        assertIssuerValidationError(result);
    }

    @Test
    void shouldRejectJwtWhenIssuerOnlyPartiallyMatchesAllowedIssuer() {
        OAuth2TokenValidatorResult result =
            validate(ALLOWED_MIGRATION_ISSUER + "/child", ALLOWED_MIGRATION_ISSUER, VALID_TOKEN_EXPIRES_AT);

        assertIssuerValidationError(result);
    }

    @Test
    void shouldRejectJwtWhenIssuerDiffersOnlyByTrailingSlash() {
        OAuth2TokenValidatorResult result =
            validate(ALLOWED_MIGRATION_ISSUER + "/", ALLOWED_MIGRATION_ISSUER, VALID_TOKEN_EXPIRES_AT);

        assertIssuerValidationError(result);
    }

    @Test
    void shouldRejectJwtWhenIssuerIsMissing() {
        OAuth2TokenValidatorResult result = validator(ALLOWED_MIGRATION_ISSUER)
            .validate(buildJwtWithoutIssuer(VALID_TOKEN_EXPIRES_AT));

        assertIssuerValidationError(result);
    }

    @Test
    void shouldRejectExpiredJwtEvenWhenIssuerMatches() {
        assertThat(validate(ENFORCED_ISSUER, ALLOWED_MIGRATION_ISSUER, EXPIRED_TOKEN_EXPIRES_AT).hasErrors())
            .isTrue();
    }

    private OAuth2TokenValidatorResult validate(String tokenIssuer, String allowedIssuers, Instant expiresAt) {
        return validator(allowedIssuers).validate(buildJwt(tokenIssuer, expiresAt));
    }

    private OAuth2TokenValidator<Jwt> validator(String allowedIssuers) {
        return SecurityConfiguration.jwtValidator(ENFORCED_ISSUER, allowedIssuers);
    }

    private void assertIssuerValidationError(OAuth2TokenValidatorResult result) {
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .anySatisfy(error -> assertThat(error.getDescription()).contains("iss"));
    }

    private Jwt buildJwt(String tokenIssuer, Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(60);
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuer(tokenIssuer)
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }

    private Jwt buildJwtWithoutIssuer(Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(60);
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }
}
