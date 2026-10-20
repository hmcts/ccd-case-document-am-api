package uk.gov.hmcts.reform.ccd.documentam.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.documentam.controller.advice.CaseDocumentControllerAdvice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SafeDependencyDiagnosticsFilterTest {

    @Test
    void dependencyWarningsKeepSeverityAndDebugCausesWhileRespectingLevels() throws Exception {
        capture((context, appender, output) -> {
            for (String name : List.of("org.apache.hc.client5.http.impl.classic.HttpRequestRetryExec",
                "org.apache.http.impl.client.DefaultHttpClient", "feign.Logger",
                "org.springframework.web.client.RestTemplate",
                "org.springframework.boot.actuate.health.HealthEndpointSupport",
                "org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]",
                "org.apache.coyote.http11.Http11Processor")) {
                Logger dependency = context.getLogger(name);
                Level previousLevel = dependency.getLevel();
                try {
                    dependency.setLevel(Level.WARN);
                    for (Level level : List.of(Level.INFO, Level.DEBUG)) {
                        output.setLevel(level);
                        appender.list.clear();
                        dependency.warn("PRIVATE_CONNECTION {}", "private-id",
                            new IllegalStateException("PRIVATE_CAUSE"));
                        assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN)
                            .singleElement().satisfies(event -> {
                                assertThat(event.getFormattedMessage())
                                    .isEqualTo("Dependency warning: code=INTERNAL_ERROR");
                                assertThat(event.getThrowableProxy()).isNull();
                            });
                        assertThat(appender.list).filteredOn(event -> event.getThrowableProxy() != null)
                            .hasSize(level == Level.DEBUG ? 1 : 0);
                        if (level == Level.DEBUG) {
                            assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.DEBUG)
                                .singleElement().satisfies(event -> {
                                    assertThat(event.getFormattedMessage()).contains("PRIVATE_CONNECTION private-id");
                                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("PRIVATE_CAUSE");
                                });
                        }
                    }
                    appender.list.clear();
                    dependency.setLevel(Level.ERROR);
                    dependency.warn("PRIVATE_FAILURE");
                    assertThat(appender.list).isEmpty();
                    dependency.error("PRIVATE_FAILURE");
                    assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.ERROR)
                        .singleElement().satisfies(event -> assertThat(event.getFormattedMessage())
                            .isEqualTo("Dependency failure: code=INTERNAL_ERROR"));
                    appender.list.clear();
                    dependency.setLevel(Level.OFF);
                    dependency.error("PRIVATE_FAILURE");
                    assertThat(appender.list).isEmpty();
                } finally {
                    dependency.setLevel(previousLevel);
                }
            }
        });
    }

    @Test
    void unknownUrlsHaveOneSafeSummaryAcrossTheMvcLogChain() throws Exception {
        capture((context, appender, output) -> {
            var mvc = MockMvcBuilders.standaloneSetup(new Probe())
                .setControllerAdvice(new CaseDocumentControllerAdvice()).build();
            for (Level level : List.of(Level.INFO, Level.DEBUG)) {
                output.setLevel(level);
                appender.list.clear();
                mvc.perform(get("/PRIVATE_IDENTIFIER"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.path").value("/"));
                assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                    .singleElement().satisfies(event -> {
                        assertThat(event.getFormattedMessage())
                            .isEqualTo("Request failed: code=INTERNAL_ERROR status=500");
                        assertThat(event.getThrowableProxy()).isNull();
                    });
            }
        });
    }

    private void capture(LogCheck check) throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger output = context.getLogger(SafeDependencyDiagnosticsFilter.class);
        Level previousLevel = output.getLevel();
        SafeDependencyDiagnosticsFilter filter = new SafeDependencyDiagnosticsFilter();
        filter.setContext(context);
        filter.start();
        context.addTurboFilter(filter);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
        try {
            check.run(context, appender, output);
        } finally {
            root.detachAppender(appender);
            appender.stop();
            context.getTurboFilterList().remove(filter);
            filter.stop();
            output.setLevel(previousLevel);
        }
    }

    @FunctionalInterface
    private interface LogCheck {
        void run(LoggerContext context, ListAppender<ILoggingEvent> appender, Logger output) throws Exception;
    }

    @RestController
    static class Probe {
        @GetMapping("/probe")
        public void ok() {
        }
    }
}
