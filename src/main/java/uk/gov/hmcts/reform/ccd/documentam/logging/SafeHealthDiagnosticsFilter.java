package uk.gov.hmcts.reform.ccd.documentam.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.FormattingTuple;
import uk.gov.hmcts.reform.ccd.documentam.security.ErrorCode;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps framework health diagnostics behind an owned DEBUG logger. */
@Slf4j
public class SafeHealthDiagnosticsFilter extends TurboFilter {

    private final Set<String> indicatorLoggers = ConcurrentHashMap.newKeySet();

    public void register(Class<?> indicatorClass) {
        for (Class<?> type = indicatorClass; type != null && type != Object.class; type = type.getSuperclass()) {
            indicatorLoggers.add(type.getName());
        }
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format,
                              Object[] arguments, Throwable exception) {
        // Null format denotes a level check, not an emitted log event.
        if (format == null || !level.isGreaterOrEqual(Level.WARN) || !indicatorLoggers.contains(logger.getName())) {
            return FilterReply.NEUTRAL;
        }
        // Turbo filters run before Logback applies the originating logger's threshold.
        if (!level.isGreaterOrEqual(logger.getEffectiveLevel())) {
            return FilterReply.DENY;
        }
        if (level == Level.ERROR) {
            log.error("Health check failed: code={}", ErrorCode.INTERNAL_ERROR);
        } else {
            log.warn("Health check failed: code={}", ErrorCode.INTERNAL_ERROR);
        }
        FormattingTuple diagnostic = MessageFormatter.arrayFormat(format, arguments);
        log.debug("Original health check failure: component={} message={}", logger.getName(),
            diagnostic.getMessage(), exception != null ? exception : diagnostic.getThrowable());
        return FilterReply.DENY;
    }
}
