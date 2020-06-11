package uk.gov.hmcts.reform.ccd.documentam.model;

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
        assertTrue(errorMap.equals(errorMap));
        assertFalse(errorMap.equals(""));
    }

    @Test
    void shouldTestHashCode() {
        int result = errorMap.hashCode();
        assertEquals(961, result);
    }

    @Test
    void shouldTestToString() {
        String result = errorMap.toString();
        assertNotNull(result);
    }
}
