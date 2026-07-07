package uk.gov.hmcts.reform.ccd.documentam.configuration;

import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigurationIT extends BaseTest {

    private static final String UNEXPECTED_ISSUER = "http://unexpected-issuer/o";
    private static final Instant TOKEN_ISSUED_AT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant VALID_TOKEN_EXPIRES_AT = Instant.parse("2099-01-01T00:00:00Z");
    private static final Instant EXPIRED_TOKEN_EXPIRES_AT = Instant.parse("2024-01-01T01:00:00Z");

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Value("${oidc.issuer}")
    private String enforcedIssuer;

    @Value("${oidc.allowed-issuers}")
    private String allowedIssuer;

    @Test
    void shouldUseJwtDecoderBeanFromSecurityConfiguration() {
        assertThat(applicationContext.getBeanNamesForType(JwtDecoder.class)).containsOnly("jwtDecoder");

        BeanDefinition jwtDecoderBeanDefinition = applicationContext.getBeanFactory().getBeanDefinition("jwtDecoder");

        assertThat(jwtDecoderBeanDefinition.getFactoryBeanName()).isEqualTo("securityConfiguration");
        assertThat(jwtDecoderBeanDefinition.getFactoryMethodName()).isEqualTo("jwtDecoder");
    }

    @Test
    void shouldDecodeJwtWhenTokenIssMatchesConfiguredIssuer() {
        String token = authToken(enforcedIssuer, VALID_TOKEN_EXPIRES_AT);

        Jwt jwt = assertDoesNotThrow(() -> jwtDecoder.decode(token));

        assertThat(jwt.getIssuer().toString()).isEqualTo(enforcedIssuer);
    }

    @Test
    void shouldDecodeJwtWhenTokenIssMatchesAllowedIssuer() {
        String token = authToken(allowedIssuer, VALID_TOKEN_EXPIRES_AT);

        Jwt jwt = assertDoesNotThrow(() -> jwtDecoder.decode(token));

        assertThat(jwt.getIssuer().toString()).isEqualTo(allowedIssuer);
    }

    @Test
    void shouldRejectJwtWhenTokenIssIsUnexpected() {
        String token = authToken(UNEXPECTED_ISSUER, VALID_TOKEN_EXPIRES_AT);

        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> jwtDecoder.decode(token)
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldRejectJwtWhenTokenIssOnlyPartiallyMatchesAllowedIssuer() {
        String token = authToken(allowedIssuer + "/child", VALID_TOKEN_EXPIRES_AT);

        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> jwtDecoder.decode(token)
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldRejectJwtWhenTokenIssIsMissing() {
        String token = authToken(null, VALID_TOKEN_EXPIRES_AT);

        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> jwtDecoder.decode(token)
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldRejectExpiredJwtEvenWhenTokenIssMatchesConfiguredIssuer() {
        String token = authToken(enforcedIssuer, EXPIRED_TOKEN_EXPIRES_AT);

        assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(token)
        );
    }

    private String authToken(String tokenIssuer, Instant expiresAt) {
        try {
            return generateAuthToken(tokenIssuer, TOKEN_ISSUED_AT, expiresAt);
        } catch (JOSEException exception) {
            throw new IllegalStateException("Failed to generate JWT for test", exception);
        }
    }
}
