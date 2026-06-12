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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigurationIT extends BaseTest {

    private static final String UNEXPECTED_ISSUER = "http://unexpected-issuer/o";

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
    void shouldDecodeJwtWhenTokenIssMatchesConfiguredIssuer() throws JOSEException {
        Jwt jwt = assertDoesNotThrow(() -> jwtDecoder.decode(generateAuthToken(AUTH_TOKEN_TTL, enforcedIssuer)));

        assertThat(jwt.getIssuer().toString()).isEqualTo(enforcedIssuer);
    }

    @Test
    void shouldDecodeJwtWhenTokenIssMatchesAllowedIssuer() throws JOSEException {
        Jwt jwt = assertDoesNotThrow(() -> jwtDecoder.decode(generateAuthToken(AUTH_TOKEN_TTL, allowedIssuer)));

        assertThat(jwt.getIssuer().toString()).isEqualTo(allowedIssuer);
    }

    @Test
    void shouldRejectJwtWhenTokenIssIsUnexpected() throws JOSEException {
        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> jwtDecoder.decode(generateAuthToken(AUTH_TOKEN_TTL, UNEXPECTED_ISSUER))
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldRejectJwtWhenTokenIssOnlyPartiallyMatchesAllowedIssuer() throws JOSEException {
        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> jwtDecoder.decode(generateAuthToken(AUTH_TOKEN_TTL, allowedIssuer + "/child"))
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldRejectJwtWhenTokenIssIsMissing() throws JOSEException {
        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> jwtDecoder.decode(generateAuthToken(AUTH_TOKEN_TTL, null))
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldRejectExpiredJwtEvenWhenTokenIssMatchesConfiguredIssuer() throws JOSEException {
        assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(generateAuthToken(-60_000, enforcedIssuer))
        );
    }
}
