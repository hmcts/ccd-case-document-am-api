package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final int status;

    private final String error;

    private final String exception;

    private final String timestamp;

    private final String path;

}
