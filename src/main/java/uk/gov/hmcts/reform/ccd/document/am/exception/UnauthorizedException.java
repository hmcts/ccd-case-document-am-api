package uk.gov.hmcts.reform.ccd.document.am.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INSUFFICIENT_PERMISSION;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {
    private static final long serialVersionUID = 3L;

    public UnauthorizedException(String message) {
        super(String.format(INSUFFICIENT_PERMISSION + " %s", message));

    }
}
