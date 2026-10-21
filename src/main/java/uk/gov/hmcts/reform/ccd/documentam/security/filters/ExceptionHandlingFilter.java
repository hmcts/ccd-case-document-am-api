package uk.gov.hmcts.reform.ccd.documentam.security.filters;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.documentam.logging.SafeFailureLogging;
import uk.gov.hmcts.reform.ccd.documentam.security.ErrorCode;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Used to catch the exceptions thrown in the Spring Security filter chain, except the following ServiceAuthFilter
 * owned exceptions. <b>uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException</b>
 * <br/><b>uk.gov.hmcts.reform.authorisation.exceptions.ServiceException</b><br/>This filter should always stay
 * at the top of the hierarchy, as it catches exceptions in the Spring Security filter chain
 */
@Slf4j
public class ExceptionHandlingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            HttpStatus status = exception instanceof ClientAbortException
                ? HttpStatus.BAD_GATEWAY : HttpStatus.INTERNAL_SERVER_ERROR;
            if (SafeFailureLogging.firstFailure(request)) {
                log.error("Security filter request failed: code={} status={}",
                    ErrorCode.fromStatus(status.value()), status.value(), exception);
            }
            if (!response.isCommitted()) {
                response.resetBuffer();
                response.setHeader("Content-Length", null);
                response.setContentType(null);
                response.setStatus(status.value());
            }
        }
    }
}
