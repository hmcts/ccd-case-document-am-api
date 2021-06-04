package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class LoggerAuditRepository implements AuditRepository {

    private final AuditLogFormatter logFormatter;

    @Autowired
    public LoggerAuditRepository(AuditLogFormatter auditLogFormatter) {
        this.logFormatter = auditLogFormatter;
    }

    @Override
    public void save(final AuditEntry auditEntry) {
        log.info(logFormatter.format(auditEntry));
    }

}
