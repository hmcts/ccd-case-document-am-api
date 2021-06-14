package uk.gov.hmcts.reform.ccd.documentam.service.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.service.ValidationUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidationUtilsTest {

    ValidationUtils validationUtils = new ValidationUtils();

    @Test
    void shouldValidate() {
        assertDoesNotThrow(() -> validationUtils.validate("1212121212121212"));
        assertThrows(BadRequestException.class, () -> validationUtils.validate("2323232323232"));
    }

    @Test
    void shouldThrowBadRequestException_ValidateDocumentId() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            validationUtils.validateDocumentId("f565abb5-c337-4ccb-ba78");
        });
    }

    @Test
    void shouldThrowBadRequestException_isValidSecurityClassification() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            validationUtils.isValidSecurityClassification("   PROTECTED");
        });
    }

    @Test
    void shouldThrowBadRequestException_ValidateLists() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            validationUtils.validateLists(new ArrayList<String>());
        });
    }

    @Test
    void shouldValidateTTL() {
        assertFalse(validationUtils.validateTTL("2021-12-31T10:10:10+"));
        assertFalse(validationUtils.validateTTL("2021-12-31T10:10:10+9999"));
        assertFalse(validationUtils.validateTTL("2021-12-31T10:10:10+999Z"));
    }
}
