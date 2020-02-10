package uk.gov.hmcts.reform.ccd.document.am.controller.advice;

public enum ErrorConstants {

    MALFORMED_JSON(1, "Malformed Input Request"),
    UNSUPPORTED_MEDIA_TYPES(2, "Unsupported Media Type"),
    INVALID_REQUEST(3,   "There is a problem with your request. Please check and try again"),
    RESOURCE_NOT_FOUND(4, "Resource not found"),
    METHOD_ARG_NOT_VALID(5, "validation on an argument failed"),
    DATA_INTEGRITY_VIOLATION(6, "attempt to insert or update data resulted in violation of an integrity constraint"),
    ILLEGAL_ARGUMENT(7, "method has been passed an illegal or inappropriate argument"),
    UNKNOWN_EXCEPTION(8, "error was caused by an unknown exception"),
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
    NO_CONTENT(204,  "No Content"),

    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),

    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    PAYMENT_REQUIRED(402, "Payment Required"),
    ACCESS_DENIED(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),

    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
        ;

    private final int errorCode;

    private final String errorMessage;

    ErrorConstants(int errorCode,String  errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage  = errorMessage;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}
