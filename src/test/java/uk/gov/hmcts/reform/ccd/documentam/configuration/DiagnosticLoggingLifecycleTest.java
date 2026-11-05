package uk.gov.hmcts.reform.ccd.documentam.configuration;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticLoggingLifecycleTest {

    @Test
    void reinstallsFiltersAfterLoggingReloadAndRemovesThemOnShutdown() {
        LoggerContext context = new LoggerContext();
        var registration = new SafeHealthLoggingConfiguration.HealthLoggingPostProcessor(context);
        try {
            var filters = java.util.List.copyOf(context.getTurboFilterList());
            assertThat(filters).hasSize(2);
            context.reset();
            context.reset();
            assertThat(context.getTurboFilterList()).containsExactlyElementsOf(filters);
            assertThat(context.getTurboFilterList()).allSatisfy(filter -> assertThat(filter.isStarted()).isTrue());
        } finally {
            registration.destroy();
        }
        assertThat(context.getTurboFilterList()).isEmpty();
        assertThat(context.getCopyOfListenerList()).isEmpty();
        context.stop();
    }
}
