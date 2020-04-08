package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

class ErrorMapTest {

    private transient ErrorMap errorMap = new ErrorMap();

    @Test
    void shouldGetCode() {
        errorMap.setCode("NOT_FOUND");
        String code = errorMap.getCode();
        assertEquals("NOT_FOUND", code);
    }

    @Test
    void shouldGetMessage() {
        errorMap.setMessage("ERROR_MESSAGE");
        String message = errorMap.getMessage();
        assertEquals("ERROR_MESSAGE", message);
    }

    @Test
    void shouldTestEquals() {
        assertTrue(errorMap.equals(new ErrorMap()));
        assertFalse(errorMap.equals(""));
    }

    @Test
    void shouldTestHashCode() {
        assertNotNull(errorMap.hashCode());
    }

    @Test
    void shouldTestToString() {
        String result = errorMap.toString();
        assertNotNull(result);
    }
}
