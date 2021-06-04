package uk.gov.hmcts.reform.ccd.documentam.auditlog;

public interface AuditRepository {

    void save(AuditEntry auditEntry);

}
