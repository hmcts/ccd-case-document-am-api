package uk.gov.hmcts.reform.ccd.document.am.controller.advice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final int errorCode;

    private final String errorMessage;

    private final String errorDescription;

    private final String timeStamp;

}
