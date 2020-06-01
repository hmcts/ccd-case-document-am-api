package uk.gov.hmcts.reform.ccd.documentam.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 8L;

    public ServiceException(final String message) {
        super(message);
    }

    public ServiceException(final String message, final Exception cause) {
        super(message, cause);
    }
}
