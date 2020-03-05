package uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.FORBIDDEN;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ForbiddenException  extends RuntimeException {

    private static final long serialVersionUID = 7L;

    public ForbiddenException(String message) {
        super(String.format(FORBIDDEN + ": %s", message));
    }
}
