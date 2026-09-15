package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class AuditLogFormatter {

    private static final String TAG = "LA-CDAM";

    private static final String COMMA = ",";
    private static final String COLON = ":";

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

    private String commaSeparatedList(List<String> list) {
        if (list == null) {
            return null;
        }

        Stream<String> stream = list.stream();
        if (this.auditLogMaxListSize > 0) {
            stream = stream.limit(this.auditLogMaxListSize);
        }

        return stream.collect(Collectors.joining(COMMA));
    }

    private String getPair(String label, String value) {
        return isNotBlank(value) ? label + COLON + value : null;
    }

}
