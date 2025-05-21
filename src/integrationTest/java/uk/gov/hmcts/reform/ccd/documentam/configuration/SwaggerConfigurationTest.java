package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigurationTest extends BaseTest {

    private static final int EXPECTED_TOTAL_ENDPOINTS = 2;
    private static final String EXPECTED_APPLICATION_CONTEXT_NAME = "CCD Case Document AM API";

    @Autowired
    private WebEndpointsSupplier webEndpointsSupplier;

    @Autowired
    private EndpointMediaTypes endpointMediaTypes;

    @Autowired
    private CorsEndpointProperties corsProperties;

    @Autowired
    private WebEndpointProperties webEndpointProperties;

    @Autowired
    private Environment environment;

    @Autowired
    private SwaggerConfiguration swaggerConfiguration;

    @Test
    public void successfullyLoadWebEndpointServletHandlerMappingTest() {
        assertThat(webEndpointsSupplier).isNotNull();
        assertThat(endpointMediaTypes).isNotNull();
        assertThat(corsProperties).isNotNull();
        assertThat(webEndpointProperties).isNotNull();
        assertThat(environment).isNotNull();
        assertThat(swaggerConfiguration).isNotNull();

        assertThat(swaggerConfiguration.webEndpointServletHandlerMapping(webEndpointsSupplier, endpointMediaTypes,
                corsProperties, webEndpointProperties, environment))
            .satisfies(mapping -> {
                assertThat(mapping.getEndpoints().size()).isEqualTo(EXPECTED_TOTAL_ENDPOINTS);
                assertThat(mapping.getApplicationContext().getId()).isEqualTo(EXPECTED_APPLICATION_CONTEXT_NAME);
            });
    }

}
