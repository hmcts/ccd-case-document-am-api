package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

public class HttpClientExceptionWrapper extends RuntimeException {
    private final String message;
    private final Throwable original;

    public HttpClientExceptionWrapper(String message, Throwable original) {
        super(message, original);
        this.message = message;
        this.original = original;
    }

    @Override
    public String getLocalizedMessage() {
        return message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public synchronized Throwable getCause() {
        return original;
    }
}
