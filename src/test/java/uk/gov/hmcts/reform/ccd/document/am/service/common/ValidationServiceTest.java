package uk.gov.hmcts.reform.ccd.document.am.service.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidationServiceTest {

    @Test
    void shouldValidate() {
        assertEquals(true, ValidationService.validate("1212121212121212"));
        assertEquals(false, ValidationService.validate("2323232323232"));
    }

    @Test
    void shouldThrowBadRequestException_ValidateDocumentId() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            ValidationService.validateDocumentId("f565abb5-c337-4ccb-ba78");
        });
    }

    @Test
    void shouldThrowBadRequestException_isValidSecurityClassification() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            ValidationService.isValidSecurityClassification("   PROTECTED");
        });
    }

    @Test
    void shouldThrowBadRequestException_ValidateLists() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            ValidationService.validateLists(new ArrayList());
        });
    }

    @Test
    void shouldValidateTTL() {
        assertEquals(false, ValidationService.validateTTL("2021-12-31T10:10:10+"));
        assertEquals(false, ValidationService.validateTTL("2021-12-31T10:10:10+9999"));
        assertEquals(false, ValidationService.validateTTL("2021-12-31T10:10:10+999Z"));
    }
}
