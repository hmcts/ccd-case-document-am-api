package uk.gov.hmcts.reform.ccd.document.am.service.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.document.am.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.service.ValidationUtils;

public class ValidationUtilsTest {

    ValidationUtils validationUtils = new ValidationUtils();

    @Test
    void shouldValidate() {
        assertTrue(validationUtils.validate("1212121212121212"));
        assertFalse(validationUtils.validate("2323232323232"));
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
            validationUtils.inputLists(new ArrayList<String>());
        });
    }

    @Test
    void shouldValidateTTL() {
        assertFalse(validationUtils.validateTTL("2021-12-31T10:10:10+"));
        assertFalse(validationUtils.validateTTL("2021-12-31T10:10:10+9999"));
        assertFalse(validationUtils.validateTTL("2021-12-31T10:10:10+999Z"));
    }
}
