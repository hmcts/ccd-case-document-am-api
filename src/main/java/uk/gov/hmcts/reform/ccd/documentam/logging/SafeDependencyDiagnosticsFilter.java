package uk.gov.hmcts.reform.ccd.documentam.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import uk.gov.hmcts.reform.ccd.documentam.security.ErrorCode;

/** Controls dependency diagnostics emitted before or outside the MVC advice. */
@Slf4j
public class SafeDependencyDiagnosticsFilter extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format,
                              Object[] arguments, Throwable exception) {
        String name = logger.getName();
        boolean boundaryLogsFailure = name.equals("org.springframework.web.servlet.PageNotFound")
            || name.startsWith("org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver")
            || name.equals("org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver");
        boolean dependency = boundaryLogsFailure
            || name.equals("org.springframework.boot.actuate.health.HealthEndpointSupport")
            || name.startsWith("org.apache.hc.")
            || name.startsWith("org.apache.http.")
            || name.startsWith("feign.")
            || name.startsWith("org.springframework.web.client.")
            || name.startsWith("org.springframework.web.multipart.")
            || name.startsWith("org.apache.catalina.core.ContainerBase.")
            || name.startsWith("org.apache.coyote.");
        if (format == null || !level.isGreaterOrEqual(Level.WARN) || !dependency) {
            return FilterReply.NEUTRAL;
        }
        if (!level.isGreaterOrEqual(logger.getEffectiveLevel())) {
            return FilterReply.DENY;
        }
        FormattingTuple diagnostic = MessageFormatter.arrayFormat(format, arguments);
        Throwable cause = exception != null ? exception : diagnostic.getThrowable();
        if (level == Level.ERROR) {
            log.error("Dependency diagnostic: code={} logger={} message={}",
                ErrorCode.INTERNAL_ERROR, logger.getName(), diagnostic.getMessage(), cause);
        } else if (level == Level.INFO) {
            log.info("Dependency diagnostic: code={} logger={} message={}",
                ErrorCode.REQUEST_FAILED, logger.getName(), diagnostic.getMessage(), cause);
        } else {
            log.warn("Dependency diagnostic: code={} logger={} message={}",
                ErrorCode.REQUEST_FAILED, logger.getName(), diagnostic.getMessage(), cause);
        }
        return FilterReply.DENY;
    }
}
