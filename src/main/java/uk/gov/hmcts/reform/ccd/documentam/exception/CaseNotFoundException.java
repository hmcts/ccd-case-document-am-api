package uk.gov.hmcts.reform.ccd.documentam.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class CaseNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 7L;

    public CaseNotFoundException(String caseReference) {
        super(String.format("Case reference %s not found for requested document.", caseReference));
    }
}
