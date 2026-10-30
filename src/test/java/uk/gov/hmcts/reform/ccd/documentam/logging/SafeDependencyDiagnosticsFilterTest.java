package uk.gov.hmcts.reform.ccd.documentam.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;


import static org.assertj.core.api.Assertions.assertThat;

class SafeDependencyDiagnosticsFilterTest {

    @Test
    void retainsDetailedFailuresAtNormalLevelsAndRespectsSourceThresholds() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        SafeDependencyDiagnosticsFilter filter = new SafeDependencyDiagnosticsFilter();

        filter.start();
        context.addTurboFilter(filter);
        Logger source = context.getLogger("org.postgresql.supportProbe");
        Logger output = context.getLogger(SafeDependencyDiagnosticsFilter.class);
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        Level previousSource = source.getLevel();
        Level previousOutput = output.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            source.setLevel(Level.WARN);
            for (Level configured : new Level[] {Level.INFO, Level.DEBUG}) {
                output.setLevel(configured);
                appender.list.clear();
                source.warn("PRIVATE diagnostic {}", "argument", new IllegalStateException("PRIVATE cause"));
                assertThat(appender.list).singleElement().satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).contains("PRIVATE diagnostic argument");
                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("PRIVATE cause");
                });
                appender.list.clear();
                source.error("PRIVATE failure", new IllegalStateException("PRIVATE error cause"));
                assertThat(appender.list).singleElement().satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(event.getFormattedMessage()).contains("PRIVATE failure");
                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("PRIVATE error cause");
                });
                source.setLevel(Level.ERROR);
                appender.list.clear();
                source.warn("disabled warning");
                assertThat(appender.list).isEmpty();
                source.setLevel(Level.WARN);
            }
        } finally {
            source.setLevel(previousSource);
            output.setLevel(previousOutput);
            root.detachAppender(appender);
            appender.stop();
            context.getTurboFilterList().remove(filter);
            filter.stop();
        }
    }

    private static class Probe {
    }
}
