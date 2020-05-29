package uk.gov.hmcts.reform.ccd.document.am.exception;

public class InvalidRequest extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRequest(String message) {
        super(message);
    }
}
