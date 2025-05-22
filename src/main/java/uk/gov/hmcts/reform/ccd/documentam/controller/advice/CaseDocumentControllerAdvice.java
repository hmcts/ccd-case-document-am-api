package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
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

        return errorDetailsResponseEntity(exception, HttpStatus.FORBIDDEN, getPath(request));
    }

    @ExceptionHandler(ResponseStatusException.class)
    protected ResponseEntity<Object> handleResponseStatusException(final ResponseStatusException exception,
                                                              final HttpServletRequest request) {

        return errorDetailsResponseEntity(exception, exception.getStatus(), getPath(request));
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
    protected ResponseEntity<Object> handleHttpClientErrorException(final HttpClientErrorException exception,
                                                                    final HttpServletRequest request) {
        HttpStatus httpStatus = getClientStatusCode(exception.getStatusCode());

        return errorDetailsResponseEntity(exception, httpStatus, getPath(request));
    }

    @ExceptionHandler(HttpServerErrorException.class)
    protected ResponseEntity<Object> handleHttpServerErrorException(final HttpServerErrorException exception,
                                                                    final HttpServletRequest request) {
        HttpStatus httpStatus = getServerStatusCode(exception.getStatusCode());

        return errorDetailsResponseEntity(exception, httpStatus, getPath(request));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleUnknownException(final Exception exception,
                                                            final HttpServletRequest request) {
        log.error(exception.getMessage(), exception);

        int status = isReadTimeoutException(exception) ? HttpStatus.GATEWAY_TIMEOUT.value()
            : HttpStatus.INTERNAL_SERVER_ERROR.value();

        return errorDetailsResponseEntity(exception, HttpStatus.valueOf(status), getPath(request));
    }

    @ExceptionHandler(FeignException.FeignClientException.class)
    public ResponseEntity<Object> handleFeignClientException(final FeignException.FeignClientException exception,
                                                             final HttpServletRequest request) {
        log.error(exception.getMessage(), exception);

        HttpStatus httpStatus = getClientStatusCode(HttpStatus.valueOf(exception.status()));

        return errorDetailsResponseEntity(exception, httpStatus, getPath(request));
    }

    @ExceptionHandler(FeignException.FeignServerException.class)
    public ResponseEntity<Object> handleFeignServerException(final FeignException.FeignServerException exception,
                                                             final HttpServletRequest request) {
        log.error(exception.getMessage(), exception);

        HttpStatus httpStatus = getServerStatusCode(HttpStatus.valueOf(exception.status()));

        return errorDetailsResponseEntity(exception, httpStatus, getPath(request));
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

    private boolean isReadTimeoutException(Throwable causeOfException) {
        //getCause() returns null if it is the same as parent Exception
        causeOfException = causeOfException.getCause() == null ? causeOfException : causeOfException.getCause();

        return causeOfException instanceof java.net.SocketTimeoutException
            && causeOfException.getMessage().contains("Read timed out");
    }

    private HttpStatus getClientStatusCode(HttpStatusCode httpStatus) {
        return httpStatus != HttpStatus.UNAUTHORIZED ? HttpStatus.INTERNAL_SERVER_ERROR 
                : HttpStatus.valueOf(httpStatus.value());
    }

    private HttpStatus getServerStatusCode(HttpStatusCode httpStatus) {
        return httpStatus == HttpStatus.INTERNAL_SERVER_ERROR ? HttpStatus.BAD_GATEWAY 
                : HttpStatus.valueOf(httpStatus.value());
    }
}
