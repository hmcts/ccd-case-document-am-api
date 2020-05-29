package uk.gov.hmcts.reform.ccd.document.am.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class ResponseFormatException extends RuntimeException {

    private static final long serialVersionUID = 7L;

    public ResponseFormatException(String  message) {
        super(message);
    }
}
