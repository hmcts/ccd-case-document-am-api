package uk.gov.hmcts.reform.ccd.documentam.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import uk.gov.hmcts.reform.ccd.documentam.logging.SafeHealthDiagnosticsFilter;

import java.util.Map;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class HealthDiagnosticRedactionTest {

    @Test
    void publicHealthRetainsStatusesWithoutPublishingDiagnostics() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        for (boolean failing : new boolean[] {false, true}) {
            new WebApplicationContextRunner().withUserConfiguration(SafeHealthLoggingConfiguration.class)
                .withConfiguration(AutoConfigurations.of(
                EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
                HealthEndpointAutoConfiguration.class))
                .withInitializer(context -> context.getEnvironment().getPropertySources().addLast(
                    new PropertiesPropertySource("application-yaml", yaml.getObject())))
                .withBean("diagnosticHealthIndicator", HealthIndicator.class,
                    () -> new DiagnosticHealthIndicator(failing))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var response = context.getBean(HealthEndpointWebExtension.class)
                        .health(ApiVersion.V3, WebServerNamespace.SERVER, SecurityContext.NONE);
                    assertThat(response.getStatus()).isEqualTo(failing ? 503 : 200);
                    assertThat(new ObjectMapper().writeValueAsString(response.getBody()))
                        .contains(failing ? "DOWN" : "UP")
                        .doesNotContain("Synthetic", "IllegalStateException", "privateId", "synthetic-case-id");
                });
        }
    }

    @Test
    void realAndNestedIndicatorDiagnosticsAreOnlyAvailableAtDebug() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        for (boolean nested : new boolean[] {false, true}) {
            new WebApplicationContextRunner().withUserConfiguration(SafeHealthLoggingConfiguration.class)
                .withConfiguration(AutoConfigurations.of(EndpointAutoConfiguration.class,
                    WebEndpointAutoConfiguration.class, HealthEndpointAutoConfiguration.class))
                .withInitializer(context -> context.getEnvironment().getPropertySources().addLast(
                    new PropertiesPropertySource("application-yaml", yaml.getObject())))
                .withBean("diagnostic", org.springframework.boot.actuate.health.HealthContributor.class,
                    () -> nested ? CompositeHealthContributor.fromMap(Map.of("nested", new NestedIndicator()))
                        : new DiagnosticHealthIndicator(true))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Logger logger = (Logger) LoggerFactory.getLogger(SafeHealthDiagnosticsFilter.class);
                    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                    Level previousLevel = logger.getLevel();
                    ListAppender<ILoggingEvent> appender = new ListAppender<>();
                    appender.start();
                    rootLogger.addAppender(appender);
                    try {
                        logger.setLevel(Level.INFO);
                        var endpoint = context.getBean(HealthEndpointWebExtension.class);
                        var response = endpoint.health(ApiVersion.V3, WebServerNamespace.SERVER, SecurityContext.NONE);
                        assertThat(response.getStatus()).isEqualTo(503);
                        assertThat(new ObjectMapper().writeValueAsString(response.getBody()))
                            .doesNotContain("Synthetic", "IllegalStateException");
                        assertThat(appender.list).hasSize(1);
                        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.WARN);
                        assertThat(appender.list.get(0).getFormattedMessage())
                            .isEqualTo("Health check failed: code=INTERNAL_ERROR");
                        assertThat(appender.list.get(0).getThrowableProxy()).isNull();
                        appender.list.clear();
                        logger.setLevel(Level.DEBUG);
                        endpoint.health(ApiVersion.V3, WebServerNamespace.SERVER, SecurityContext.NONE);
                        assertThat(appender.list).hasSize(2);
                        assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.DEBUG);
                        assertThat(appender.list.get(1).getThrowableProxy().getMessage())
                            .isEqualTo("Synthetic private health diagnostic");
                    } finally {
                        logger.setLevel(previousLevel);
                        rootLogger.detachAppender(appender);
                        appender.stop();
                    }
                });
        }
    }

    static class DiagnosticHealthIndicator extends AbstractHealthIndicator {
        private final boolean failing;

        DiagnosticHealthIndicator(boolean failing) {
            this.failing = failing;
        }

        @Override
        protected void doHealthCheck(Health.Builder builder) {
            if (failing) {
                throw new IllegalStateException("Synthetic private health diagnostic");
            }
            builder.up();
        }
    }

    static class NestedIndicator extends DiagnosticHealthIndicator {
        NestedIndicator() {
            super(true);
        }
    }
}
