package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorMapTest {

    private transient ErrorMap errorMap = new ErrorMap();

    @Test
    void shouldGetCode() {
        errorMap.setCode("NOT_FOUND");
        String code = errorMap.getCode();
        assertEquals(code, "NOT_FOUND");
    }

    @Test
    void shouldGetMessage() {
        errorMap.setMessage("ERROR_MESSAGE");
        String message = errorMap.getMessage();
        assertEquals(message, "ERROR_MESSAGE");
    }

    @Test
    void shouldTestEquals() {
        assertNotNull(errorMap.equals(new ErrorMap()));
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
