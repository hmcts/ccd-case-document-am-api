package uk.gov.hmcts.reform.ccd.documentam.configuration.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MultiIssuerValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger LOG = LoggerFactory.getLogger(MultiIssuerValidator.class);
    private static final OAuth2Error ERROR = new OAuth2Error(
        "invalid_token",
        "The required issuer is missing or invalid",
        null
    );

    private final Set<String> normalizedValidIssuers;

    public MultiIssuerValidator(List<String> validIssuers) {
        Assert.notEmpty(validIssuers, "Valid issuers should not be null or empty.");
        this.normalizedValidIssuers = validIssuers.stream()
            .map(this::normalizeIssuer)
            .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Assert.notEmpty(this.normalizedValidIssuers, "At least one valid issuer must be non-blank.");
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
        String normalizedIssuer = normalizeIssuer(issuer);

        if (normalizedIssuer != null && normalizedValidIssuers.contains(normalizedIssuer)) {
            return OAuth2TokenValidatorResult.success();
        }

        LOG.warn("Invalid issuer: raw='{}', normalized='{}'", issuer, normalizedIssuer);
        return OAuth2TokenValidatorResult.failure(ERROR);
    }

    private String normalizeIssuer(String issuer) {
        if (issuer == null) {
            return null;
        }

        String normalized = issuer.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }
}
