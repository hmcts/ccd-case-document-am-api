package uk.gov.hmcts.reform.ccd.documentam.auditlog.aop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.AuditOperationType;

import java.util.List;

@Builder(builderMethodName = "auditContextWith")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditContext {

    private List<String> documentIds;
    private String caseId;
    private AuditOperationType auditOperationType;

    private int httpStatus;
    private String httpMethod;
    private String requestPath;
    private String requestId;

    private String invokingService;
    private String jurisdiction;
    private String caseType;

}
