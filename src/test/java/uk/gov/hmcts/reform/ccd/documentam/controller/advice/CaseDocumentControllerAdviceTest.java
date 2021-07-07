package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.gov.hmcts.reform.ccd.documentam.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.UnauthorizedException;

public class CaseDocumentControllerAdviceTest {

    private transient CaseDocumentControllerAdvice csda = new CaseDocumentControllerAdvice();

    private transient HttpServletRequest servletRequestMock = mock(HttpServletRequest.class);


    @Test
    public void handleUnauthorizedExceptionException() {
        UnauthorizedException unauthorizedException = mock(UnauthorizedException.class);
        ResponseEntity<Object> responseEntity = csda
            .handleUnauthorizedException(unauthorizedException);
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleRequiredFieldMissingException() {
        RequiredFieldMissingException requiredFieldMissingException = mock(RequiredFieldMissingException.class);
        ResponseEntity<Object> responseEntity = csda
            .handleRequiredFieldMissingException(requiredFieldMissingException);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void customValidationError() {
        InvalidRequest invalidRequestException = mock(InvalidRequest.class);
        ResponseEntity<Object> responseEntity = csda.customValidationError(invalidRequestException);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleMethodArgumentNotValidException() {
        MethodArgumentNotValidException methodArgumentNotValidException =
            mock(MethodArgumentNotValidException.class);
        ResponseEntity<Object> responseEntity = csda
            .handleMethodArgumentNotValidException(methodArgumentNotValidException);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleResourceNotFoundException() {
        ResourceNotFoundException resourceNotFoundException = mock(ResourceNotFoundException.class);
        ResponseEntity<Object> responseEntity = csda
            .handleResourceNotFoundException(resourceNotFoundException);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleHttpMessageConversionException() {
        HttpMessageConversionException httpMessageConversionException = mock(HttpMessageConversionException.class);
        ResponseEntity<Object> responseEntity = csda
            .handleHttpMessageConversionException(httpMessageConversionException);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleUnknownException() {
        Exception exception = mock(Exception.class);
        ResponseEntity<Object> responseEntity = csda.handleUnknownException(exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());

    }
}
