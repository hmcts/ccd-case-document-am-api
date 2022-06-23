package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.controller.endpoints.CaseDocumentAmController;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.UnauthorizedException;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaseDocumentControllerAdviceTest implements TestFixture {

    private final CaseDocumentControllerAdvice underTest = new CaseDocumentControllerAdvice();

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void handleUnauthorizedExceptionException() {
        UnauthorizedException unauthorizedException = mock(UnauthorizedException.class);

        ResponseEntity<Object> responseEntity = underTest.handleUnauthorizedException(unauthorizedException, request);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    void handleForbiddenExceptionException() {
        final ForbiddenException forbiddenException = mock(ForbiddenException.class);

        final ResponseEntity<Object> responseEntity = underTest.handleForbiddenException(forbiddenException, request);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    void handleBadRequestExceptionException() {
        final BadRequestException badRequestException = mock(BadRequestException.class);

        testBadRequest(badRequestException);
    }

    @Test
    void handleMissingRequestParameterExceptionException() {
        final MissingServletRequestParameterException missingServletRequestParameterException =
            mock(MissingServletRequestParameterException.class);

        testBadRequest(missingServletRequestParameterException);
    }

    @Test
    void handleMethodArgumentTypeMismatchExceptionException() {
        final MethodArgumentTypeMismatchException methodArgumentTypeMismatchException =
            mock(MethodArgumentTypeMismatchException.class);

        testBadRequest(methodArgumentTypeMismatchException);
    }

    @Test
    void handleRequiredFieldMissingException() {
        final RequiredFieldMissingException requiredFieldMissingException = mock(RequiredFieldMissingException.class);

        testBadRequest(requiredFieldMissingException);
    }

    @Test
    void customValidationError() {
        final InvalidRequest invalidRequestException = mock(InvalidRequest.class);

        testBadRequest(invalidRequestException);
    }

    @Test
    void handleHttpMessageConversionException() {
        final HttpMessageConversionException httpMessageConversionException =
            mock(HttpMessageConversionException.class);

        testBadRequest(httpMessageConversionException);
    }

    @Test
    void handleResourceNotFoundException() {
        ResourceNotFoundException resourceNotFoundException = mock(ResourceNotFoundException.class);

        ResponseEntity<Object> responseEntity = underTest
            .handleResourceNotFoundException(resourceNotFoundException, request);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    void handleUnknownException() {
        Exception exception = mock(Exception.class);

        ResponseEntity<Object> responseEntity = underTest.handleUnknownException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());
    }

    private void testBadRequest(final Exception exceptionClazz) {
        final ResponseEntity<Object> responseEntity = underTest.handleBadRequestException(exceptionClazz, request);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    void testHandleMethodArgumentNotValidException() throws Exception {
        final CaseDocumentAmController controller = new CaseDocumentAmController(
            mock(DocumentManagementService.class),
            mock(SecurityUtils.class)
        );

        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(underTest)
            .build();

        final CaseDocumentsMetadata body = new CaseDocumentsMetadata(
            CASE_ID_VALUE,
            CASE_TYPE_ID_VALUE,
            JURISDICTION_ID_VALUE,
            emptyList()
        );

        mockMvc.perform(patch("/cases/documents/attachToCase")
                                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                        .content(TestFixture.objectToJsonString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("At least one document should be provided")))
            .andExpect(result -> assertThat(result.getResolvedException())
                .isNotNull()
                .satisfies(throwable -> assertThat(throwable).isInstanceOf(MethodArgumentNotValidException.class)));
    }

    @Test
    void testHandleHttpClientErrorException() {
        final HttpClientErrorException exception = HttpClientErrorException
            .create(HttpStatus.BAD_REQUEST, "myUniqueExceptionMessage", HttpHeaders.EMPTY,
                    new byte[0], Charset.defaultCharset());

        final ResponseEntity<Object> responseEntity = underTest.handleHttpClientErrorException(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCodeValue());

        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();
        assert errorResponse != null;
        assertTrue(errorResponse.getError().contains("myUniqueExceptionMessage"));
    }

    @Test
    void testHandleHttpServerErrorExceptionWhenReceivedInternalServerError() {
        final HttpServerErrorException exception = HttpServerErrorException
                .create(HttpStatus.INTERNAL_SERVER_ERROR, "myUniqueExceptionMessage", HttpHeaders.EMPTY,
                        new byte[0], Charset.defaultCharset());

        final ResponseEntity<Object> responseEntity = underTest.handleHttpServerErrorException(exception, request);

        assertEquals(HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_GATEWAY.value(), responseEntity.getStatusCodeValue());

        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();
        assert errorResponse != null;
        assertTrue(errorResponse.getError().contains("myUniqueExceptionMessage"));
    }

    @Test
    void testHandleHttpServerErrorExceptionWhenReceivedServiceUnavailable() {
        final HttpServerErrorException exception = HttpServerErrorException
                .create(HttpStatus.SERVICE_UNAVAILABLE, "myUniqueExceptionMessage", HttpHeaders.EMPTY,
                        new byte[0], Charset.defaultCharset());

        final ResponseEntity<Object> responseEntity = underTest.handleHttpServerErrorException(exception, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, responseEntity.getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), responseEntity.getStatusCodeValue());

        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();
        assert errorResponse != null;
        assertTrue(errorResponse.getError().contains("myUniqueExceptionMessage"));
    }
}
