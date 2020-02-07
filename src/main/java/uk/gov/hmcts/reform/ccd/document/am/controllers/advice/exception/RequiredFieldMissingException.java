package uk.gov.hmcts.reform.ccd.document.am.controllers.advice.exception;

public class RequiredFieldMissingException extends RuntimeException {

    private static final long serialVersionUID = 3L;

    public RequiredFieldMissingException(String message) {
        super(message);
    }
}
