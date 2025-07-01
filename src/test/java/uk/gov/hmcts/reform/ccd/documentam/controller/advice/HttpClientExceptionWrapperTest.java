package uk.gov.hmcts.reform.ccd.documentam.controller.advice;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class HttpClientExceptionWrapperTest {

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "Test message";
        Throwable cause = new RuntimeException("Original exception");

        HttpClientExceptionWrapper wrapper = new HttpClientExceptionWrapper(message, cause);

        assertEquals(message, wrapper.getMessage());
        assertEquals(message, wrapper.getLocalizedMessage());
        assertEquals(cause, wrapper.getCause());
    }

    @Test
    void shouldPreserveOriginalCause() {
        Throwable cause = new RuntimeException("Original");
        HttpClientExceptionWrapper wrapper = new HttpClientExceptionWrapper("message", cause);

        assertSame(cause, wrapper.getCause());
    }

    @Test
    void shouldReturnConsistentMessages() {
        String message = "Error message";
        HttpClientExceptionWrapper wrapper = new HttpClientExceptionWrapper(message, null);

        assertEquals(message, wrapper.getMessage());
        assertEquals(message, wrapper.getLocalizedMessage());
    }

    @Test
    void shouldAllowNullCause() {
        HttpClientExceptionWrapper wrapper = new HttpClientExceptionWrapper("message", null);

        assertNull(wrapper.getCause());
    }

    @Test
    void shouldAllowNullMessage() {
        HttpClientExceptionWrapper wrapper = new HttpClientExceptionWrapper(null, new RuntimeException());

        assertNull(wrapper.getMessage());
        assertNull(wrapper.getLocalizedMessage());
    }
}
