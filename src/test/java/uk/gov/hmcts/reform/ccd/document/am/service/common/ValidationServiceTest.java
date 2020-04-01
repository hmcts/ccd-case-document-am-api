package uk.gov.hmcts.reform.ccd.document.am.service.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidationServiceTest {

    @Test
    void shouldValidate() {
        assertEquals(true, ValidationService.validate("1212121212121212"));
        assertEquals(false, ValidationService.validate(null));
        assertEquals(false, ValidationService.validate("2323232323232"));
    }
}
