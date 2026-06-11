package uk.gov.hmcts.reform.ccd.documentam.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OidcIssuerConfigurationTest {

    @Test
    void shouldReturnOnlyPrimaryIssuerWhenAllowedIssuersUnset() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", null))
            .containsExactly("primary");
    }

    @Test
    void shouldReturnOnlyPrimaryIssuerWhenAllowedIssuersBlank() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", " "))
            .containsExactly("primary");
    }

    @Test
    void shouldIncludePrimaryAndConfiguredAllowedIssuers() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", " secondary, tertiary , secondary "))
            .containsExactly("primary", "secondary", "tertiary");
    }

    @Test
    void shouldIgnoreEmptyAllowedIssuerEntryBeforeComma() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", ", secondary"))
            .containsExactly("primary", "secondary");
    }

    @Test
    void shouldIgnoreEmptyAllowedIssuerEntryAfterComma() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", "secondary,"))
            .containsExactly("primary", "secondary");
    }

    @Test
    void shouldIgnoreEmptyAllowedIssuerEntryBetweenCommas() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", "secondary,,tertiary"))
            .containsExactly("primary", "secondary", "tertiary");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectMissingPrimaryIssuerEvenWhenAllowedIssuersAreConfigured(String primaryIssuer) {
        assertThatThrownBy(() -> OidcIssuerConfiguration.allowedIssuers(primaryIssuer, "secondary"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("oidc.issuer must not be blank");
    }
}
