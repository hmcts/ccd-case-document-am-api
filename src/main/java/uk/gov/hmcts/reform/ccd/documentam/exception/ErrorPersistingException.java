package uk.gov.hmcts.reform.ccd.documentam.exception;

public class ErrorPersistingException extends RuntimeException {

    private static final long serialVersionUID = 2L;

    public ErrorPersistingException(String message) {
        super(message);
    }
}
