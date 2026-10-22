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

/** Retains framework health diagnostics in administrator-controlled server logs. */
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
        FormattingTuple diagnostic = MessageFormatter.arrayFormat(format, arguments);
        Throwable cause = exception != null ? exception : diagnostic.getThrowable();
        if (level == Level.ERROR) {
            log.error("Health check failure: code={} logger={} message={}",
                ErrorCode.INTERNAL_ERROR, logger.getName(), diagnostic.getMessage(), cause);
        } else if (level == Level.INFO) {
            log.info("Health check failure: code={} logger={} message={}",
                ErrorCode.REQUEST_FAILED, logger.getName(), diagnostic.getMessage(), cause);
        } else {
            log.warn("Health check failure: code={} logger={} message={}",
                ErrorCode.REQUEST_FAILED, logger.getName(), diagnostic.getMessage(), cause);
        }
        return FilterReply.DENY;
    }
}
