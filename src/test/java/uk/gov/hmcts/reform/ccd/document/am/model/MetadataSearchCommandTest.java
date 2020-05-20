package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

class MetadataSearchCommandTest {

    private transient MetadataSearchCommand metadataSearch = new MetadataSearchCommand();

    @Test
    void shouldGetName() {
        metadataSearch.setName("METADATA");
        String name = metadataSearch.getName();
        assertEquals("METADATA", name);
    }

    @Test
    void shouldGetValue() {
        metadataSearch.setValue("DOCUMENT");
        String value = metadataSearch.getValue();
        assertEquals("DOCUMENT", value);
    }

    @Test
    void shouldTestEquals() {
        assertTrue(metadataSearch.equals(new MetadataSearchCommand()));
        assertTrue(metadataSearch.equals(metadataSearch));
        assertFalse(metadataSearch.equals(""));
    }

    @Test
    void shouldTestHashCode() {
        int result = metadataSearch.hashCode();
        assertEquals(961, result);
    }

    @Test
    void shouldTestToString() {
        String result = metadataSearch.toString();
        assertNotNull(result);
    }
}
