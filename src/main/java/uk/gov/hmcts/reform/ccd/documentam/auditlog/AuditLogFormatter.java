package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import com.microsoft.applicationinsights.core.dependencies.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
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
        List<String> formattedPairs = Lists.newArrayList(
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
        );

        CollectionUtils.filter(formattedPairs, PredicateUtils.notNullPredicate());

        return TAG + " " + String.join(COMMA, formattedPairs);
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
