package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import lombok.Data;

import java.util.List;

@Data
public class AuditEntry {

    private String dateTime;

    private String operationType;

    private String idamId;
    private String invokingService;
    private String jurisdiction;
    private String caseType;

    private int httpStatus;
    private String httpMethod;
    private String requestPath;
    private String requestId;

    private List<String> documentIds;
    private List<String> caseIds;

}
