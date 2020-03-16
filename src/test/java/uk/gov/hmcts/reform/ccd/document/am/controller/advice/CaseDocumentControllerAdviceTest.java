package uk.gov.hmcts.reform.ccd.document.am.controller.advice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.gov.hmcts.reform.ccd.document.am.controller.WelcomeController;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.UnauthorizedException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;

public class CaseDocumentControllerAdviceTest {

    private transient CaseDocumentControllerAdvice csda = new CaseDocumentControllerAdvice();

    private transient HttpServletRequest servletRequestMock = mock(HttpServletRequest.class);

    private transient WelcomeController welcomeController = new WelcomeController();

    @Test
    public void handleUnautorizedExceptionException() {
        UnauthorizedException unauthorizedException = mock(UnauthorizedException.class);
        ResponseEntity<Object> responseEntity = csda.handleUnautorizedExceptionException(servletRequestMock, unauthorizedException);
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED.value(),responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleRequiredFieldMissingException() {
        RequiredFieldMissingException requiredFieldMissingException = mock(RequiredFieldMissingException.class);
        ResponseEntity<Object> responseEntity = csda.handleRequiredFieldMissingException(servletRequestMock, requiredFieldMissingException);
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
        MethodArgumentNotValidException methodArgumentNotValidException = mock(MethodArgumentNotValidException.class);
        ResponseEntity<Object> responseEntity = csda.handleMethodArgumentNotValidException(servletRequestMock, methodArgumentNotValidException);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleResourceNotFoundException() {
        ResourceNotFoundException resourceNotFoundException = mock(ResourceNotFoundException.class);
        ResponseEntity<Object> responseEntity = csda.handleResourceNotFoundException(servletRequestMock,resourceNotFoundException);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleHttpMessageConversionException() {
        HttpMessageConversionException httpMessageConversionException = mock(HttpMessageConversionException.class);
        ResponseEntity<Object> responseEntity = csda.handleHttpMessageConversionException(servletRequestMock, httpMessageConversionException);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleUnknownException() {
        Exception exception = mock(Exception.class);
        ResponseEntity<Object> responseEntity = csda.handleUnknownException(servletRequestMock, exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());

    }

    @Test
    public void getTimeStamp() {
        String time = csda.getTimeStamp();
        assertEquals(time.substring(0,16), new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH).format(new Date()));
    }

    @Test
    void testHandleRequiredFieldMissingException() {
        Assertions.assertThrows(RequiredFieldMissingException.class, () -> {
            welcomeController.getException("requiredFieldMissingException");
        });
    }

    @Test
    void testInvalidRequest() {
        Assertions.assertThrows(InvalidRequest.class, () -> {
            welcomeController.getException("invalidRequest");
        });
    }

    @Test
    void testResourceNotFoundException() {
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            welcomeController.getException("resourceNotFoundException");
        });
    }

    @Test
    void testHttpMessageConversionException() {
        Assertions.assertThrows(HttpMessageConversionException.class, () -> {
            welcomeController.getException("httpMessageConversionException");
        });
    }

    @Test
    void testBadRequestException() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            welcomeController.getException("badRequestException");
        });
    }

    @Test
    void testCaseNotFoundException() {
        Assertions.assertThrows(CaseNotFoundException.class, () -> {
            welcomeController.getException("caseNotFoundException");
        });
    }
}
