package uk.gov.hmcts.reform.ccd.documentam.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NativeDiagnosticLoggingTest {

    @Test
    void retainsNativeCategoriesAndDiagnosticsAtEveryConfiguredLevel() {
        LoggerContext context = new LoggerContext();
        try {
            final var source = context.getLogger("health.NativeIndicator");
            final var unrelated = context.getLogger("application.DiagnosticReplacement");
            var accepted = new ListAppender<ILoggingEvent>();
            accepted.start();
            var rejected = new ListAppender<ILoggingEvent>();
            rejected.addFilter(new Filter<ILoggingEvent>() {
                @Override
                public FilterReply decide(ILoggingEvent event) {
                    return FilterReply.DENY;
                }
            });
            rejected.start();
            source.setAdditive(false);
            unrelated.setAdditive(false);
            source.addAppender(accepted);
            unrelated.addAppender(rejected);
            unrelated.setLevel(Level.DEBUG);
            var cause = new IllegalStateException("Private diagnostic");
            for (Level threshold : List.of(Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF)) {
                source.setLevel(threshold);
                accepted.list.clear();
                source.debug("Diagnostic DEBUG", cause);
                source.info("Diagnostic INFO", cause);
                source.warn("Diagnostic WARN", cause);
                source.error("Diagnostic ERROR", cause);
                assertThat(accepted.list).extracting(ILoggingEvent::getLevel).containsExactlyElementsOf(
                    List.of(Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR).stream()
                        .filter(level -> level.isGreaterOrEqual(threshold)).toList());
                assertThat(accepted.list).allSatisfy(event -> {
                    assertThat(event.getLoggerName()).isEqualTo(source.getName());
                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("Private diagnostic");
                    assertThat(event.getThrowableProxy().getStackTraceElementProxyArray()).isNotEmpty();
                });
                assertThat(rejected.list).isEmpty();
            }
        } finally {
            context.stop();
        }
    }
}
