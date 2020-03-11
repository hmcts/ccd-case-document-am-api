package uk.gov.hmcts.reform.ccd.document.am.service.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationServiceTest {

    private transient ValidationService validationService = new ValidationService();

    @Test
    void shouldValidate() {
        assertEquals(validationService.validate("1212121212121212"), true);
        assertEquals(validationService.validate(null), false);
        assertEquals(validationService.validate("2323232323232"), false);
    }
}
