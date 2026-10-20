package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.documentam.logging.SafeFailureLogging;
import uk.gov.hmcts.reform.ccd.documentam.security.ErrorCode;
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
import org.springframework.web.servlet.HandlerMapping;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Slf4j
@ControllerAdvice
public class CaseDocumentControllerAdvice {


    @ExceptionHandler(UnauthorizedException.class)
    protected ResponseEntity<Object> handleUnauthorizedException(final UnauthorizedException exception,
                                                                 final HttpServletRequest request) {

        return errorDetailsResponseEntity(exception, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    protected ResponseEntity<Object> handleForbiddenException(final ForbiddenException exception,
                                                              final HttpServletRequest request) {

        return errorDetailsResponseEntity(exception, HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    protected ResponseEntity<Object> handleResponseStatusException(final ResponseStatusException exception,
                                                              final HttpServletRequest request) {

        ResponseEntity<Object> response = errorDetailsResponseEntity(exception, exception.getStatusCode(),
            request);
        return ResponseEntity.status(exception.getStatusCode()).headers(exception.getHeaders())
            .body(response.getBody());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Object> handleMethodArgumentNotValidException(
        final MethodArgumentNotValidException exception,
        final HttpServletRequest request
    ) {
        return errorDetailsResponseEntity(exception, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({BadRequestException.class,
        RequiredFieldMissingException.class,
        InvalidRequest.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageConversionException.class})
    protected ResponseEntity<Object> handleBadRequestException(final Exception exception,
                                                               final HttpServletRequest request) {
        return errorDetailsResponseEntity(exception, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleResourceNotFoundException(final ResourceNotFoundException exception,
                                                                     final HttpServletRequest request) {
        return errorDetailsResponseEntity(exception, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    protected ResponseEntity<Object> handleHttpClientErrorException(final HttpClientErrorException exception,
                                                                    final HttpServletRequest request) {
        HttpStatus httpStatus = getClientStatusCode(exception.getStatusCode());

        return errorDetailsResponseEntity(exception, httpStatus, request);
    }

    @ExceptionHandler(HttpClientErrorException.UnprocessableEntity.class)
    protected ResponseEntity<Object> handleHttpClientUnprocessableEntityException(final HttpClientErrorException ex,
                                                                    final HttpServletRequest request) {
        return errorDetailsResponseEntity(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    protected ResponseEntity<Object> handleHttpServerErrorException(final HttpServerErrorException exception,
                                                                    final HttpServletRequest request) {
        HttpStatus httpStatus = getServerStatusCode(exception.getStatusCode());

        return errorDetailsResponseEntity(exception, httpStatus, request);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleUnknownException(final Exception exception,
                                                            final HttpServletRequest request) {

        int status = isReadTimeoutException(exception) ? HttpStatus.GATEWAY_TIMEOUT.value()
            : HttpStatus.INTERNAL_SERVER_ERROR.value();

        ResponseEntity<Object> response = errorDetailsResponseEntity(exception, HttpStatus.valueOf(status),
            request);
        // Preserve this advice's original status, while retaining required framework headers.
        if (exception instanceof org.springframework.web.ErrorResponse error) {
            return ResponseEntity.status(status).headers(error.getHeaders())
                .body(response.getBody());
        }
        return response;
    }

    @ExceptionHandler(FeignException.FeignClientException.class)
    public ResponseEntity<Object> handleFeignClientException(final FeignException.FeignClientException exception,
                                                             final HttpServletRequest request) {

        HttpStatus httpStatus = getClientStatusCode(HttpStatus.valueOf(exception.status()));

        return errorDetailsResponseEntity(exception, httpStatus, request);
    }

    @ExceptionHandler(FeignException.FeignServerException.class)
    public ResponseEntity<Object> handleFeignServerException(final FeignException.FeignServerException exception,
                                                             final HttpServletRequest request) {

        HttpStatus httpStatus = getServerStatusCode(HttpStatus.valueOf(exception.status()));

        return errorDetailsResponseEntity(exception, httpStatus, request);
    }

    private String getTimeStamp() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.ENGLISH).format(new Date());
    }

    private ResponseEntity<Object> errorDetailsResponseEntity(final Exception ex,
                                                              final HttpStatusCode httpStatus,
                                                              final HttpServletRequest request) {
        if (SafeFailureLogging.firstFailure(request)) {
            log.error("Request failed: code={} status={}",
                ErrorCode.fromStatus(httpStatus.value()), httpStatus.value());
            log.debug("Original request failure: code={} status={}",
                ErrorCode.fromStatus(httpStatus.value()), httpStatus.value(), ex);
        }
        final ErrorResponse errorDetails = ErrorResponse.builder()
            .status(httpStatus.value())
            .path(getPath(request))
            .error(safeMessage(httpStatus))
            .timestamp(getTimeStamp())
            .build();

        return ResponseEntity.status(httpStatus).body(errorDetails);
    }

    private String safeMessage(HttpStatusCode status) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        return knownStatus == null ? "Request failed" : knownStatus.getReasonPhrase();
    }

    private String getPath(final HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String route ? route : "/";
    }

    private boolean isReadTimeoutException(Throwable causeOfException) {
        //getCause() returns null if it is the same as parent Exception
        causeOfException = causeOfException.getCause() == null ? causeOfException : causeOfException.getCause();

        return causeOfException instanceof java.net.SocketTimeoutException
            && String.valueOf(causeOfException.getMessage()).contains("Read timed out");
    }

    private HttpStatus getClientStatusCode(HttpStatusCode httpStatus) {
        return httpStatus.value() == HttpStatus.UNAUTHORIZED.value()
            ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private HttpStatus getServerStatusCode(HttpStatusCode httpStatus) {
        return httpStatus == HttpStatus.INTERNAL_SERVER_ERROR ? HttpStatus.BAD_GATEWAY
                : HttpStatus.valueOf(httpStatus.value());
    }
}
