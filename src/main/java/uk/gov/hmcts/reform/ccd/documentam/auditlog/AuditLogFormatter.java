package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class AuditLogFormatter {

    private static final String TAG = "LA-CDAM";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final int auditLogMaxListSize;

    @Autowired
    public AuditLogFormatter(@Value("${audit.log.max-list-size:0}") int auditLogMaxListSize) {
        this.auditLogMaxListSize = auditLogMaxListSize;
    }

    public String format(AuditEntry entry) {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("tag", TAG);
        add(logEntry, "dateTime", entry.getDateTime());
        add(logEntry, "operationType", entry.getOperationType());
        add(logEntry, "idamId", entry.getIdamId());
        add(logEntry, "invokingService", entry.getInvokingService());
        add(logEntry, "endpointCalled", buildEndpoint(entry));
        add(logEntry, "operationalOutcome", entry.getHttpStatus());
        add(logEntry, "documentId", limitedList(entry.getDocumentIds()));
        add(logEntry, "jurisdiction", entry.getJurisdiction());
        add(logEntry, "caseType", entry.getCaseType());
        add(logEntry, "caseId", entry.getCaseId());
        add(logEntry, "X-Request-ID", entry.getRequestId());
        try {
            return objectMapper.writeValueAsString(logEntry);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to format audit log entry", e);
        }
    }

    private String buildEndpoint(AuditEntry entry) {
        if (isBlank(entry.getHttpMethod()) || isBlank(entry.getRequestPath())) {
            return null;
        }
        return entry.getHttpMethod() + " " + entry.getRequestPath();
    }

    private List<String> limitedList(List<String> list) {
        if (list == null) {
            return null;
        }
        if (this.auditLogMaxListSize > 0) {
            return list.stream().limit(this.auditLogMaxListSize).toList();
        }
        return list;
    }

    private void add(Map<String, Object> logEntry, String label, @Nullable Object value) {
        if (value instanceof String && isBlank((String) value)) {
            return;
        }
        if (value != null) {
            logEntry.put(label, value);
        }
    }

}
