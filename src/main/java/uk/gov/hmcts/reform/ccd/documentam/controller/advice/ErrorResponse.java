package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.ccd.documentam.security.ErrorCode;

@Getter
@Builder
public class ErrorResponse {

    private final int status;

    private final String error;

    public ErrorCode getCode() {
        return ErrorCode.fromStatus(status);
    }

    private final String timestamp;

    private final String path;

}
