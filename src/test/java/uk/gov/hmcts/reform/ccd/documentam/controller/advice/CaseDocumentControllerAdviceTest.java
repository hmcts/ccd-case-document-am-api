package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.UnauthorizedException;

public class CaseDocumentControllerAdviceTest {

    private final CaseDocumentControllerAdvice underTest = new CaseDocumentControllerAdvice();

    @Test
    public void handleUnauthorizedExceptionException() {
        UnauthorizedException unauthorizedException = mock(UnauthorizedException.class);
        ResponseEntity<Object> responseEntity = underTest
            .handleUnauthorizedException(unauthorizedException);
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleForbiddenExceptionException() {
        final ForbiddenException forbiddenException = mock(ForbiddenException.class);

        final ResponseEntity<Object> responseEntity = underTest.handleForbiddenException(forbiddenException);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleBadRequestExceptionException() {
        final BadRequestException badRequestException = mock(BadRequestException.class);

        final ResponseEntity<Object> responseEntity = underTest.handleBadRequestException(badRequestException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleMissingRequestParameterExceptionException() {
        final MissingServletRequestParameterException missingServletRequestParameterException =
            mock(MissingServletRequestParameterException.class);

        final ResponseEntity<Object> responseEntity =
            underTest.handleMissingRequestParameterException(missingServletRequestParameterException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleMethodArgumentTypeMismatchExceptionException() {
        final MethodArgumentTypeMismatchException methodArgumentTypeMismatchException =
            mock(MethodArgumentTypeMismatchException.class);

        final ResponseEntity<Object> responseEntity =
            underTest.handleMethodArgumentTypeMismatchException(methodArgumentTypeMismatchException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleRequiredFieldMissingException() {
        RequiredFieldMissingException requiredFieldMissingException = mock(RequiredFieldMissingException.class);
        ResponseEntity<Object> responseEntity = underTest
            .handleRequiredFieldMissingException(requiredFieldMissingException);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void customValidationError() {
        InvalidRequest invalidRequestException = mock(InvalidRequest.class);
        ResponseEntity<Object> responseEntity = underTest.customValidationError(invalidRequestException);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleMethodArgumentNotValidException() {
        MethodArgumentNotValidException methodArgumentNotValidException =
            mock(MethodArgumentNotValidException.class);
        ResponseEntity<Object> responseEntity = underTest
            .handleMethodArgumentNotValidException(methodArgumentNotValidException);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleResourceNotFoundException() {
        ResourceNotFoundException resourceNotFoundException = mock(ResourceNotFoundException.class);
        ResponseEntity<Object> responseEntity = underTest
            .handleResourceNotFoundException(resourceNotFoundException);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleHttpMessageConversionException() {
        HttpMessageConversionException httpMessageConversionException = mock(HttpMessageConversionException.class);
        ResponseEntity<Object> responseEntity = underTest
            .handleHttpMessageConversionException(httpMessageConversionException);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleUnknownException() {
        Exception exception = mock(Exception.class);
        ResponseEntity<Object> responseEntity = underTest.handleUnknownException(exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());

    }
}
