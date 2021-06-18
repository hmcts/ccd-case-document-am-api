package uk.gov.hmcts.reform.ccd.documentam.wiremock;

import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.ccd.documentam.wiremock.extension.ConnectionClosedTransformer;
import uk.gov.hmcts.reform.ccd.documentam.wiremock.extension.DynamicOAuthJwkSetResponseTransformer;
import uk.gov.hmcts.reform.ccd.documentam.wiremock.extension.DynamicS2sDetailsResponseTransformer;

@Configuration
public class WireMockTestConfiguration {

    @Bean
    WireMockConfigurationCustomizer optionsCustomizer() {
        return options -> {
            options.extensions(
                new ConnectionClosedTransformer(),
                new DynamicS2sDetailsResponseTransformer(),
                new DynamicOAuthJwkSetResponseTransformer()
            );
        };
    }

}
