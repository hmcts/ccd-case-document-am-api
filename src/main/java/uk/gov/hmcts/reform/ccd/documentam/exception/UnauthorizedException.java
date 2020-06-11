package uk.gov.hmcts.reform.ccd.documentam.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {
    private static final long serialVersionUID = 3L;

    public UnauthorizedException(String message) {
        super(String.format(Constants.INSUFFICIENT_PERMISSION + " %s", message));

    }
}
