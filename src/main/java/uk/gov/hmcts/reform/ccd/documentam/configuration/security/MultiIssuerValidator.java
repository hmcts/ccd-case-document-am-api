package uk.gov.hmcts.reform.ccd.documentam.configuration.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

import java.util.List;

public class MultiIssuerValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger LOG = LoggerFactory.getLogger(MultiIssuerValidator.class);
    private static final OAuth2Error ERROR = new OAuth2Error(
        "invalid_token",
        "The required issuer is missing or invalid",
        null
    );

    private final List<String> validIssuers;

    public MultiIssuerValidator(List<String> validIssuers) {
        Assert.notEmpty(validIssuers, "Valid issuers should not be null or empty.");
        this.validIssuers = List.copyOf(validIssuers);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
        if (issuer != null && validIssuers.contains(issuer)) {
            return OAuth2TokenValidatorResult.success();
        }

        LOG.warn("Invalid issuer: {}", issuer);
        return OAuth2TokenValidatorResult.failure(ERROR);
    }
}
