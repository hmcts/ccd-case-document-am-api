package uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 4L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
