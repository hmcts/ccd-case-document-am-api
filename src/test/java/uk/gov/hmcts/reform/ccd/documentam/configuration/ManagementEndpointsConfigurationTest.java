package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManagementEndpointsConfigurationTest {

    @Test
    void shouldDisableLoggersEndpointAndLimitExposedActuators() {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new ClassPathResource("application.yaml"));

        var properties = yamlPropertiesFactoryBean.getObject();

        assertThat(properties)
            .isNotNull();
        assertThat(properties.getProperty("management.endpoint.loggers.enabled"))
            .isEqualTo("false");
        assertThat(List.of(properties.getProperty("management.endpoints.web.exposure.include").split(",\\s*")))
            .containsExactly("health", "info", "prometheus")
            .doesNotContain("loggers");
    }
}