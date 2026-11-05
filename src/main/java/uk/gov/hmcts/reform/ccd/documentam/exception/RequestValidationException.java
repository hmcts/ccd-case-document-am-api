package uk.gov.hmcts.reform.ccd.documentam.exception;

import org.springframework.validation.BindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.util.Set;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_ID_MISSING;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_ID_NOT_VALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID_MISSING;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CLASSIFICATION_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CLASSIFICATION_MISSING;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID_MISSING;

public final class RequestValidationException extends BadRequestException {

    private static final long serialVersionUID = 1L;

    private static final Set<String> SAFE_MESSAGES = Set.of(
        CASE_ID_MISSING, CASE_ID_NOT_VALID, CASE_TYPE_ID_INVALID, CASE_TYPE_ID_MISSING,
        CLASSIFICATION_ID_INVALID, CLASSIFICATION_MISSING, JURISDICTION_ID_INVALID, JURISDICTION_ID_MISSING,
        "Provide the TTL", "At least one document should be provided",
        "Provide some file to be uploaded", "Please provide at least one file to be uploaded."
    );

    public RequestValidationException(BindingResult result) {
        super(safeMessage(result), new BindException(result));
    }

    public static String safeMessage(BindingResult result) {
        return result.getAllErrors().stream()
            .filter(error -> !(error instanceof FieldError fieldError) || !fieldError.isBindingFailure())
            .map(error -> error.getDefaultMessage())
            .filter(message -> message != null && SAFE_MESSAGES.contains(message))
            .findFirst()
            .orElse("Bad Request");
    }
}
