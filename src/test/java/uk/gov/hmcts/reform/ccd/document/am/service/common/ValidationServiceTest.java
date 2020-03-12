package uk.gov.hmcts.reform.ccd.document.am.service.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidationServiceTest {

    @Test
    void shouldValidate() {
        assertEquals(ValidationService.validate("1212121212121212"), true);
        assertEquals(ValidationService.validate(null), false);
        assertEquals(ValidationService.validate("2323232323232"), false);
    }
}
