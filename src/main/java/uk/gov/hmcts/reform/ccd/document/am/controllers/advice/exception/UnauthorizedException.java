package uk.gov.hmcts.reform.ccd.document.am.controllers.advice.exception;

public class UnauthorizedException extends RuntimeException {
    private static final long serialVersionUID = 3L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
