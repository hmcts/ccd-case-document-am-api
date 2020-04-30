package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UpdateDocumentCommandTest {

    private transient UpdateDocumentCommand updateDocumentCommand = new UpdateDocumentCommand();

    @Test
    void shouldGetTtl() {
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/YYYY", Locale.ENGLISH);
        updateDocumentCommand.setTtl("01/01/1970");
        assertEquals("01/01/1970", updateDocumentCommand.getTtl());
    }

    @Test
    void shouldTestEquals() {
        assertTrue(updateDocumentCommand.equals(new UpdateDocumentCommand()));
        assertFalse(updateDocumentCommand.equals(""));
    }

    @Test
    void shouldTestEqualsTrue() {
        assertTrue(updateDocumentCommand.equals(updateDocumentCommand));
    }

    @Test
    void shouldTestHashCode() {
        assertNotNull(updateDocumentCommand.hashCode());
    }

    @Test
    void shouldTestToString() {
        String result = updateDocumentCommand.toString();
        assertNotNull(result);
    }

    @Test
    void shouldTestHashCodeValue() {
        //this passes because ttl is always null in the class as it has no context
        int result = updateDocumentCommand.hashCode();
        assertEquals(31, result);
    }
}
