package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.aop.AuditContext;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.aop.AuditContextHolder;
import uk.gov.hmcts.reform.ccd.documentam.configuration.AuditConfiguration;

import java.util.List;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
public class AuditInterceptor implements AsyncHandlerInterceptor {

    private final AuditService auditService;
    private final boolean auditLogEnabled;
    private final List<Integer> auditLogIgnoreStatuses;

    public AuditInterceptor(final AuditService auditService,
                            final boolean auditLogEnabled,
                            final List<Integer> auditLogIgnoreStatuses) {
        this.auditService = auditService;
        this.auditLogEnabled = auditLogEnabled;
        this.auditLogIgnoreStatuses = auditLogIgnoreStatuses;
    }

    @Override
    public void afterCompletion(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final Object handler,
                                @Nullable final Exception ex) {

        if (auditLogEnabled && hasAuditAnnotation(handler)) {
            if (!isHttpStatusIgnored(response.getStatus())) {

                var auditContext = AuditContextHolder.getAuditContext();
                auditContext = populateHttpSemantics(auditContext, request, response);

                var logAuditAnnotation = ((HandlerMethod) handler).getMethodAnnotation((LogAudit.class));
                if (logAuditAnnotation != null) {
                    auditContext.setAuditOperationType(logAuditAnnotation.operationType());
                }

                try {
                    auditService.audit(auditContext);
                } catch (Exception e) {  // Ignoring audit failures
                    log.error("Error while auditing the request data:{}", e.getMessage());
                }
            }

            AuditContextHolder.remove();
        }
    }

    private boolean hasAuditAnnotation(Object handler) {
        return handler instanceof HandlerMethod && ((HandlerMethod) handler).hasMethodAnnotation(LogAudit.class);
    }

    private boolean isHttpStatusIgnored(int status) {
        return auditLogIgnoreStatuses.contains(status);
    }

    private AuditContext populateHttpSemantics(AuditContext auditContext,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        AuditContext context = Optional.ofNullable(auditContext).orElse(new AuditContext());
        context.setHttpStatus(response.getStatus());
        context.setHttpMethod(request.getMethod());
        context.setRequestPath(request.getRequestURI());
        context.setRequestId(request.getHeader(AuditConfiguration.REQUEST_ID));
        context.setInvokingService(auditService.getInvokingService(request));
        return context;
    }

}
