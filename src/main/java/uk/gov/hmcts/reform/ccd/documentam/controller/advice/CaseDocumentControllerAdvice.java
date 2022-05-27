package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.util.UriUtils;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.UnauthorizedException;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Slf4j
@ControllerAdvice
public class CaseDocumentControllerAdvice {

    private static final String LOG_STRING = "handling exception: ";
    private static final Logger logger = LoggerFactory.getLogger(CaseDocumentControllerAdvice.class);

    @ExceptionHandler(UnauthorizedException.class)
    protected ResponseEntity<Object> handleUnauthorizedException(final UnauthorizedException exception,
                                                                 final HttpServletRequest request) {

        return errorDetailsResponseEntity(exception, HttpStatus.UNAUTHORIZED, getPath(request));
    }

    @ExceptionHandler(ForbiddenException.class)
    protected ResponseEntity<Object> handleForbiddenException(final ForbiddenException exception,
                                                              final HttpServletRequest request) {

        logger.error("I AM IN handleForbiddenException", exception);
        return errorDetailsResponseEntity(exception, HttpStatus.FORBIDDEN, getPath(request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Object> handleMethodArgumentNotValidException(
        final MethodArgumentNotValidException exception,
        final HttpServletRequest request
    ) {
        logger.error(LOG_STRING, exception);

        final String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .findFirst()
            .orElse(null);

        final ErrorResponse errorDetails = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .path(getPath(request))
            .exception(exception.getClass().getName())
            .error(message)
            .timestamp(getTimeStamp())
            .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({BadRequestException.class,
        RequiredFieldMissingException.class,
        InvalidRequest.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageConversionException.class})
    protected ResponseEntity<Object> handleBadRequestException(final Exception exception,
                                                               final HttpServletRequest request) {
        return errorDetailsResponseEntity(exception, HttpStatus.BAD_REQUEST, getPath(request));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleResourceNotFoundException(final ResourceNotFoundException exception,
                                                                     final HttpServletRequest request) {
        return errorDetailsResponseEntity(exception, HttpStatus.NOT_FOUND, getPath(request));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    protected ResponseEntity<Object> handleHttpClientErrorException(final HttpClientErrorException exception) {
        logger.error("I AM IN handleForbiddenException", exception);
        return new ResponseEntity<>(exception.getResponseBodyAsString(), exception.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleUnknownException(final Exception exception,
                                                            final HttpServletRequest request) {
        return errorDetailsResponseEntity(exception, HttpStatus.INTERNAL_SERVER_ERROR, getPath(request));
    }

    private String getTimeStamp() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.ENGLISH).format(new Date());
    }

    private ResponseEntity<Object> errorDetailsResponseEntity(final Exception ex,
                                                              final HttpStatus httpStatus,
                                                              final String requestPath) {
        logger.error(LOG_STRING, ex);
        final ErrorResponse errorDetails = ErrorResponse.builder()
            .status(httpStatus.value())
            .path(requestPath)
            .error(ex.getLocalizedMessage())
            .exception(ex.getClass().getName())
            .timestamp(getTimeStamp())
            .build();

        return new ResponseEntity<>(errorDetails, httpStatus);
    }

    private String getPath(final HttpServletRequest request) {
        return UriUtils.encodePath(request.getRequestURI(), StandardCharsets.UTF_8);
    }

}
