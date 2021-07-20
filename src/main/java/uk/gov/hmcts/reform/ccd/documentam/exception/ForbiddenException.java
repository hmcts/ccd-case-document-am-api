package uk.gov.hmcts.reform.ccd.documentam.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ForbiddenException  extends RuntimeException {

    private static final long serialVersionUID = 7L;

    public ForbiddenException(String message) {
        super(String.format(Constants.FORBIDDEN + ": %s", message));
    }

    public ForbiddenException(Throwable exception) {
        super(Constants.FORBIDDEN, exception);
    }

    public ForbiddenException(String message, Throwable exception) {
        super(String.format(Constants.FORBIDDEN + ": %s", message), exception);
    }
}
