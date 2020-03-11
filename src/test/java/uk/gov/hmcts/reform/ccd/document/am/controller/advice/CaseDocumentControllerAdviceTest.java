package uk.gov.hmcts.reform.ccd.document.am.controller.advice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConversionException;
import uk.gov.hmcts.reform.ccd.document.am.controller.WelcomeController;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.CaseNotFoundException;

class CaseDocumentControllerAdviceTest {

    private transient WelcomeController welcomeController = new WelcomeController();

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
