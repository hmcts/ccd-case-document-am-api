package uk.gov.hmcts.reform.ccd.documentam.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class SafeHealthDiagnosticsFilterTest {

    @Test
    void respectsTheOriginalLoggerThresholdAndPreservesSeverity() {
        Logger indicator = (Logger) LoggerFactory.getLogger(SyntheticIndicator.class);
        Logger output = (Logger) LoggerFactory.getLogger(SafeHealthDiagnosticsFilter.class);
        LoggerContext context = indicator.getLoggerContext();
        Level previousIndicatorLevel = indicator.getLevel();
        Level previousOutputLevel = output.getLevel();
        SafeHealthDiagnosticsFilter filter = new SafeHealthDiagnosticsFilter();
        filter.register(SyntheticIndicator.class);
        filter.setContext(context);
        filter.start();
        context.addTurboFilter(filter);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
        try {
            output.setLevel(Level.INFO);
            indicator.setLevel(Level.OFF);
            indicator.warn("Synthetic private health diagnostic");
            indicator.error("Synthetic private health diagnostic");
            assertThat(appender.list).isEmpty();
            indicator.setLevel(Level.ERROR);
            indicator.warn("Synthetic private health diagnostic");
            assertThat(appender.list).isEmpty();
            indicator.error("Synthetic private health diagnostic", new IllegalStateException("private detail"));
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
            assertThat(appender.list.get(0).getFormattedMessage())
                .isEqualTo("Health check failed: code=INTERNAL_ERROR");
            assertThat(appender.list.get(0).getThrowableProxy()).isNull();
            appender.list.clear();
            indicator.setLevel(Level.WARN);
            indicator.warn("Synthetic private health diagnostic");
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.WARN);
            assertThat(appender.list.get(0).getFormattedMessage())
                .isEqualTo("Health check failed: code=INTERNAL_ERROR");
        } finally {
            indicator.setLevel(previousIndicatorLevel);
            output.setLevel(previousOutputLevel);
            root.detachAppender(appender);
            appender.stop();
            context.getTurboFilterList().remove(filter);
            filter.stop();
        }
    }

    static class SyntheticIndicator {
    }
}
