package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetadataSearchCommandTest {

    private transient MetadataSearchCommand metadataSearch = new MetadataSearchCommand();

    @Test
    void shouldGetName() {
        metadataSearch.setName("METADATA");
        String name = metadataSearch.getName();
        assertEquals(name, "METADATA");
    }

    @Test
    void shouldGetValue() {
        metadataSearch.setValue("DOCUMENT");
        String value = metadataSearch.getValue();
        assertEquals(value, "DOCUMENT");
    }

    @Test
    void shouldTestEquals() {
        assertNotNull(metadataSearch.equals(new MetadataSearchCommand()));
    }

    @Test
    void shouldTestHashCode() {
        assertNotNull(metadataSearch.hashCode());
    }

    @Test
    void shouldTestToString() {
        String result = metadataSearch.toString();
        assertNotNull(result);
    }
}
