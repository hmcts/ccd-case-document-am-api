package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.UnauthorizedException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@ControllerAdvice
// FIXME : https://tools.hmcts.net/jira/browse/RDM-11324
public class CaseDocumentControllerAdvice {

    private static final String LOG_STRING = "handling exception: ";
    private static final Logger logger = LoggerFactory.getLogger(CaseDocumentControllerAdvice.class);

    @ExceptionHandler(UnauthorizedException.class)
    protected ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException exception) {
        return errorDetailsResponseEntity(exception,
                                          HttpStatus.UNAUTHORIZED,
                                          ErrorConstants.UNAUTHORIZED.getErrorCode(),
                                          ErrorConstants.UNAUTHORIZED.getErrorMessage()
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    protected ResponseEntity<Object> handleForbiddenException(ForbiddenException exception) {
        return errorDetailsResponseEntity(exception, HttpStatus.FORBIDDEN,
                                          ErrorConstants.ACCESS_DENIED.getErrorCode(),
                                          ErrorConstants.ACCESS_DENIED.getErrorMessage()
        );
    }

    @ExceptionHandler({BadRequestException.class,
        RequiredFieldMissingException.class,
        InvalidRequest.class,
        MethodArgumentNotValidException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageConversionException.class})
    protected ResponseEntity<Object> handleBadRequestException(final Exception exception) {
        return errorDetailsResponseEntity(
            exception,
            BAD_REQUEST,
            ErrorConstants.BAD_REQUEST.getErrorCode(),
            ErrorConstants.BAD_REQUEST.getErrorMessage()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException exception) {
        return errorDetailsResponseEntity(exception, HttpStatus.NOT_FOUND,
            ErrorConstants.NOT_FOUND.getErrorCode(), ErrorConstants.NOT_FOUND.getErrorMessage());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleUnknownException(Exception exception) {
        return errorDetailsResponseEntity(exception,
            HttpStatus.INTERNAL_SERVER_ERROR, ErrorConstants.UNKNOWN_EXCEPTION.getErrorCode(),
            ErrorConstants.UNKNOWN_EXCEPTION.getErrorMessage());
    }

    private String getTimeStamp() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.ENGLISH).format(new Date());
    }

    private ResponseEntity<Object> errorDetailsResponseEntity(final Exception ex,
                                                              final HttpStatus httpStatus,
                                                              final int errorCode,
                                                              final String errorMsg) {
        logger.error(LOG_STRING, ex);
        final ErrorResponse errorDetails = ErrorResponse.builder()
            .errorCode(errorCode)
            .errorMessage(errorMsg)
            .errorDescription(ex.getLocalizedMessage())
            .timeStamp(getTimeStamp())
            .build();

        return new ResponseEntity<>(errorDetails, httpStatus);
    }
}
