package uk.gov.hmcts.reform.ccd.documentam.auditlog.aop;

public class AuditContextHolder {

    private AuditContextHolder() {
    }

    private static final InheritableThreadLocal<AuditContext> threadLocal = new InheritableThreadLocal<>();

    public static void setAuditContext(AuditContext auditContext) {
        threadLocal.set(auditContext);
    }

    public static AuditContext getAuditContext() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }

}
