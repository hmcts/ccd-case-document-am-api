package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoggerAuditRepositoryTest {

    @InjectMocks
    private LoggerAuditRepository underTest;

    @Mock
    private AuditLogFormatter logFormatter;

    @Test
    @DisplayName("Should save audit entry by using AuditLogFormatter")
    void shouldSaveAuditEntry() {

        // GIVEN
        AuditEntry auditEntry = new AuditEntry();

        // WHEN
        underTest.save(auditEntry);

        // THEN
        verify(logFormatter).format(auditEntry);

    }

}
