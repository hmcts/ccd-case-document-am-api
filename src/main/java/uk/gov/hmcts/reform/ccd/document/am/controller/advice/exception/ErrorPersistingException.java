package uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception;

public class ErrorPersistingException extends RuntimeException {

    private static final long serialVersionUID = 2L;

    public ErrorPersistingException(String message) {
        super(message);
    }
}
