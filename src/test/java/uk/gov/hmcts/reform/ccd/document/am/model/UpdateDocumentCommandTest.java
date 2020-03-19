package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertNotNull(updateDocumentCommand.equals(new UpdateDocumentCommand()));
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
}
