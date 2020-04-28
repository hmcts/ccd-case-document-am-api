package uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.RESOURCE_NOT_FOUND;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 4L;

    public ResourceNotFoundException(String message) {
        super(String.format(RESOURCE_NOT_FOUND + " %s", message));
    }
}
