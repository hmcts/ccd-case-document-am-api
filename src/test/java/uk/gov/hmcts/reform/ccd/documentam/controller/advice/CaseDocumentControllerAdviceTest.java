package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import feign.FeignException;
import feign.Request;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
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
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaseDocumentControllerAdviceTest implements TestFixture {

    private final CaseDocumentControllerAdvice underTest = new CaseDocumentControllerAdvice();

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void handleUnauthorizedException() {
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
    void handleResponseStatusExceptionException() {
        final ResponseStatusException responseStatusException = mock(ResponseStatusException.class);
        when(responseStatusException.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(responseStatusException.getMessage()).thenReturn("Internal Server Error");

        final ResponseEntity<Object> responseEntity = underTest.handleResponseStatusException(responseStatusException,
                                                                                              request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());
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
            mock(SecurityUtils.class),
            mock(ApplicationParams.class)
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

    @Test
    void testHandleHttpClientErrorExceptionWhenReceivedInternalServerError() {
        final HttpClientErrorException exception = HttpClientErrorException
            .create(HttpStatus.INTERNAL_SERVER_ERROR, "myUniqueExceptionMessage", HttpHeaders.EMPTY,
                    new byte[0], Charset.defaultCharset());

        final ResponseEntity<Object> responseEntity = underTest.handleHttpClientErrorException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());

        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();
        assert errorResponse != null;
        assertTrue(errorResponse.getError().contains("myUniqueExceptionMessage"));
    }

    @Test
    void testHandleHttpClientErrorExceptionWhenReceivedUnauthorized() {
        final HttpClientErrorException exception = HttpClientErrorException
            .create(HttpStatus.UNAUTHORIZED, "myUniqueExceptionMessage", HttpHeaders.EMPTY,
                    new byte[0], Charset.defaultCharset());

        final ResponseEntity<Object> responseEntity = underTest.handleHttpClientErrorException(exception, request);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), responseEntity.getStatusCodeValue());

        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();
        assert errorResponse != null;
        assertTrue(errorResponse.getError().contains("myUniqueExceptionMessage"));
    }

    @Test
    void testHandleHttpClientErrorExceptionWhenReceivedMovedPermanently() {
        final HttpClientErrorException exception = HttpClientErrorException
            .create(HttpStatus.MOVED_PERMANENTLY, "myUniqueExceptionMessage", HttpHeaders.EMPTY,
                    new byte[0], Charset.defaultCharset());

        final ResponseEntity<Object> responseEntity = underTest.handleHttpClientErrorException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());

        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();
        assert errorResponse != null;
        assertTrue(errorResponse.getError().contains("myUniqueExceptionMessage"));
    }


    @Test
    public void handleFeignServerException_shouldSwitch500_502() throws IOException {
        FeignException.FeignServerException ex = new FeignException.FeignServerException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
            Request.create(Request.HttpMethod.GET, "Internal Server Error", Map.of(), new byte[0],
                           Charset.defaultCharset(), null), new byte[0], Map.of());
        final ResponseEntity<Object> responseEntity = underTest.handleFeignServerException(ex, request);

        assertEquals(HttpStatus.BAD_GATEWAY.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    public void handleFeignServerException_shouldReturn5xx() throws IOException {
        FeignException.FeignServerException ex = new FeignException.FeignServerException(
            HttpStatus.GATEWAY_TIMEOUT.value(), "Gateway Timeout",
            Request.create(Request.HttpMethod.GET, "Gateway Timeout", Map.of(), new byte[0],
                           Charset.defaultCharset(), null), new byte[0], Map.of());
        final ResponseEntity<Object> response = underTest.handleFeignServerException(ex, request);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT.value(), response.getStatusCodeValue());
    }

    @Test
    public void handleFeignClientException_shouldReturn401() {
        FeignException.FeignClientException ex = new FeignException.FeignClientException(
            HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
            Request.create(Request.HttpMethod.GET, "UNAUTHORIZED", Map.of(), new byte[0],
                           Charset.defaultCharset(), null), new byte[0], Map.of());

        final ResponseEntity<Object> response = underTest.handleFeignClientException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
    }

    @Test
    public void handleFeignClientException_shouldReturn500IfReceived400() {
        FeignException.FeignClientException ex = new FeignException.FeignClientException(
            HttpStatus.BAD_REQUEST.value(), "Bad Request",
            Request.create(Request.HttpMethod.GET, "Bad Request", Map.of(), new byte[0],
                           Charset.defaultCharset(), null), new byte[0], Map.of());

        final ResponseEntity<Object> response = underTest
            .handleFeignClientException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCodeValue());
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength") // don't want to break long method name
    public void handleSocketTimeoutException_shouldReturnInternalServerErrorIfTypeNotReadTimedOut() {
        String myUniqueExceptionMessage = "My unique generic runtime exception message";

        SocketTimeoutException socketTimeoutException = new SocketTimeoutException("some timeout error");
        ResourceAccessException exception = new ResourceAccessException(myUniqueExceptionMessage,
                                                                                      socketTimeoutException);

        final ResponseEntity<Object> responseEntity = underTest.handleUnknownException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength") // don't want to break long method name
    public void handleUnknownExceptionException_shouldReturnInternalServerErrorIfIOExceptionTypeNotReadTimedOut() {
        String myUniqueExceptionMessage = "My unique generic runtime exception message";

        SocketTimeoutException socketTimeoutException = new SocketTimeoutException("some timeout error");
        IOException ioException = new IOException(myUniqueExceptionMessage, socketTimeoutException);

        final ResponseEntity<Object> responseEntity = underTest.handleUnknownException(ioException, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength") // don't want to break long method name
    public void handleUnknownException_shouldReturnBadGatewayIfIOExceptionTypeReadTimedOut() {
        String myUniqueExceptionMessage = "My unique generic runtime exception message";

        SocketTimeoutException socketTimeoutException = new SocketTimeoutException("Read timed out");
        IOException ioException = new IOException(myUniqueExceptionMessage, socketTimeoutException);

        final ResponseEntity<Object> responseEntity = underTest.handleUnknownException(ioException, request);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, responseEntity.getStatusCode());
        assertEquals(HttpStatus.GATEWAY_TIMEOUT.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength") // don't want to break long method name
    public void handleUnknownException_shouldReturnInternalServerErrorIfSocketTimeoutExceptionTypeNotReadTimedOut() {
        SocketTimeoutException socketTimeoutException = new SocketTimeoutException("some timeout error");

        final ResponseEntity<Object> responseEntity = underTest.handleUnknownException(socketTimeoutException, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength") // don't want to break long method name
    public void handleUnknownException_shouldReturnBadGatewayIfSocketTimeoutExceptionTypeReadTimedOut() {
        SocketTimeoutException socketTimeoutException = new SocketTimeoutException("Read timed out");
        final ResponseEntity<Object> responseEntity = underTest.handleUnknownException(socketTimeoutException, request);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, responseEntity.getStatusCode());
        assertEquals(HttpStatus.GATEWAY_TIMEOUT.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength") // don't want to break long method name
    public void handleFeignServerException_shouldReturnInternalServerErrorWhenReceivedFeignServerExceptionForbidden() {
        String myUniqueExceptionMessage = "My unique generic runtime exception message 1";
        final FeignException.FeignServerException exception =
            createFeignServerException(HttpStatus.FORBIDDEN, myUniqueExceptionMessage);

        final ResponseEntity<Object> responseEntity = underTest.handleUnknownException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());
    }

    private FeignException.FeignServerException createFeignServerException(final HttpStatus httpStatus,
                                                                           final String message) {
        return new FeignException.FeignServerException(httpStatus.value(), message,
                                                       Request.create(Request.HttpMethod.GET, message, Map.of(),
                                                                      new byte[0], Charset.defaultCharset(), null),
                                                       new byte[0], new HashMap<>());
    }
}
