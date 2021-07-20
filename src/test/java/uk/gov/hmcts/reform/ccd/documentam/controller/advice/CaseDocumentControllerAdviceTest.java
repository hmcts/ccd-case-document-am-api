package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaseDocumentControllerAdviceTest implements TestFixture {

    private final CaseDocumentControllerAdvice underTest = new CaseDocumentControllerAdvice();

    @Test
    void handleUnauthorizedExceptionException() {
        UnauthorizedException unauthorizedException = mock(UnauthorizedException.class);
        ResponseEntity<Object> responseEntity = underTest
            .handleUnauthorizedException(unauthorizedException);
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    void handleForbiddenExceptionException() {
        final ForbiddenException forbiddenException = mock(ForbiddenException.class);

        final ResponseEntity<Object> responseEntity = underTest.handleForbiddenException(forbiddenException);

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
            .handleResourceNotFoundException(resourceNotFoundException);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getStatusCodeValue());
    }

    @Test
    void handleUnknownException() {
        Exception exception = mock(Exception.class);
        ResponseEntity<Object> responseEntity = underTest.handleUnknownException(exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getStatusCodeValue());
    }

    private void testBadRequest(final Exception exceptionClazz) {
        final ResponseEntity<Object> responseEntity = underTest.handleBadRequestException(exceptionClazz);

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
            .andExpect(jsonPath("$.errorDescription", is("At least one document should be provided")))
            .andExpect(result -> assertThat(result.getResolvedException())
                .isNotNull()
                .satisfies(throwable -> assertThat(throwable).isInstanceOf(MethodArgumentNotValidException.class)));
    }
}
