package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
public class CaseDocumentControllerAdvice {

    private static final String LOG_STRING = "handling exception: ";
    private static final Logger logger = LoggerFactory.getLogger(CaseDocumentControllerAdvice.class);

    @ExceptionHandler(UnauthorizedException.class)
    protected ResponseEntity<Object> handleUnAutorizedExceptionException(UnauthorizedException exception) {
        return errorDetailsResponseEntity(exception,
                                          HttpStatus.UNAUTHORIZED,
                                          ErrorConstants.UNAUTHORIZED.getErrorCode(),
                                          ErrorConstants.UNAUTHORIZED.getErrorMessage()
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    protected ResponseEntity<Object> handleUnAutorizedExceptionException(ForbiddenException exception) {
        return errorDetailsResponseEntity(exception, HttpStatus.FORBIDDEN,
                                          ErrorConstants.ACCESS_DENIED.getErrorCode(),
                                          ErrorConstants.ACCESS_DENIED.getErrorMessage()
        );
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<Object> badRequestException(BadRequestException ex) {
        return errorDetailsResponseEntity(ex, BAD_REQUEST,
                                          ErrorConstants.BAD_REQUEST.getErrorCode(),
                                          ErrorConstants.BAD_REQUEST.getErrorMessage()
        );
    }

    @ExceptionHandler(RequiredFieldMissingException.class)
    protected ResponseEntity<Object> handleRequiredFieldMissingException(RequiredFieldMissingException exception) {
        return errorDetailsResponseEntity(exception, BAD_REQUEST,
            ErrorConstants.INVALID_REQUEST.getErrorCode(), ErrorConstants.INVALID_REQUEST.getErrorMessage());
    }

    @ExceptionHandler(InvalidRequest.class)
    public ResponseEntity<Object> customValidationError(InvalidRequest ex) {
        return errorDetailsResponseEntity(ex, BAD_REQUEST,
            ErrorConstants.INVALID_REQUEST.getErrorCode(), ErrorConstants.INVALID_REQUEST.getErrorMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        return errorDetailsResponseEntity(exception, BAD_REQUEST,
            ErrorConstants.INVALID_REQUEST.getErrorCode(), ErrorConstants.INVALID_REQUEST.getErrorMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException exception) {
        return errorDetailsResponseEntity(
            exception,
            BAD_REQUEST,
            ErrorConstants.INVALID_REQUEST.getErrorCode(),
            ErrorConstants.INVALID_REQUEST.getErrorMessage()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException exception) {
        return errorDetailsResponseEntity(exception, HttpStatus.NOT_FOUND,
            ErrorConstants.NOT_FOUND.getErrorCode(), ErrorConstants.NOT_FOUND.getErrorMessage());
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    protected ResponseEntity<Object> handleHttpMessageConversionException(HttpMessageConversionException exception) {
        return errorDetailsResponseEntity(exception, BAD_REQUEST,
            ErrorConstants.INVALID_REQUEST.getErrorCode(), ErrorConstants.INVALID_REQUEST.getErrorMessage());
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

    private ResponseEntity<Object> errorDetailsResponseEntity(Exception ex, HttpStatus httpStatus, int errorCode,
                                                              String errorMsg) {

        logger.error(LOG_STRING, ex);
        ErrorResponse errorDetails = ErrorResponse.builder()
            .errorCode(errorCode)
            .errorMessage(errorMsg)
            .errorDescription(ex.getLocalizedMessage())
            .timeStamp(getTimeStamp())
            .build();
        return new ResponseEntity<>(
            errorDetails, httpStatus);
    }
}
