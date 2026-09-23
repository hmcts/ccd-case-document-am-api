package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        String formattedPairs = Stream.of(
            getPair("dateTime", entry.getDateTime()),
            getPair("operationType", entry.getOperationType()),
            getPair("idamId", entry.getIdamId()),
            getPair("invokingService", entry.getInvokingService()),
            getPair("endpointCalled", entry.getHttpMethod() + " " + entry.getRequestPath()),
            getPair("operationalOutcome", String.valueOf(entry.getHttpStatus())),
            getPair("documentId", commaSeparatedList(entry.getDocumentIds())),
            getPair("jurisdiction", entry.getJurisdiction()),
            getPair("caseType", entry.getCaseType()),
            getPair("caseId", entry.getCaseId()),
            getPair("X-Request-ID", entry.getRequestId())
        )
            .filter(Objects::nonNull)
            .collect(Collectors.joining(COMMA));

        return TAG + " " + formattedPairs;
    }

    private String buildEndpoint(AuditEntry entry) {
        if (isBlank(entry.getHttpMethod()) || isBlank(entry.getRequestPath())) {
            return null;
        }
        return entry.getHttpMethod() + " " + entry.getRequestPath();
    }

    private List<String> limitedList(List<String> list) {
        if (list == null) {
            return List.of();
        }
        if (this.auditLogMaxListSize > 0) {
            return list.stream().limit(this.auditLogMaxListSize).toList();
        }
        return list;
    }

    private void add(Map<String, Object> logEntry, String label, @Nullable Object value) {
        if (value instanceof String string && isBlank(string)) {
            return;
        }
        if (value instanceof Collection<?> collection && collection.isEmpty()) {
            return;
        }
        if (value != null) {
            logEntry.put(label, value);
        }
    }

}
