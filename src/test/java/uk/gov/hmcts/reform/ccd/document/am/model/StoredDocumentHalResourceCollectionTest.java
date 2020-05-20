package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

class StoredDocumentHalResourceCollectionTest {

    private transient StoredDocumentHalResourceCollection storedDocumentHalResourceCollection = new StoredDocumentHalResourceCollection();

    @Test
    void shouldAddContentItem() {
        List<String> roles = Arrays.asList("citizen");
        StoredDocumentHalResourceCollection payloadBody = storedDocumentHalResourceCollection.addContentItem(new StoredDocumentHalResource());
        assertNotNull(storedDocumentHalResourceCollection.getContent());
        assertEquals(payloadBody.getClass(), StoredDocumentHalResourceCollection.class);
    }

    @Test
    void shouldGetContent() {
        List<StoredDocumentHalResource> resouce = Arrays.asList(new StoredDocumentHalResource());
        storedDocumentHalResourceCollection.setContent(resouce);
        assertNotNull(storedDocumentHalResourceCollection.getContent());
    }

    @Test
    void shouldTestEquals() {
        assertTrue(storedDocumentHalResourceCollection.equals(new StoredDocumentHalResourceCollection()));
        assertFalse(storedDocumentHalResourceCollection.equals(""));
    }

    @Test
    void shouldEqualTrue() {
        assertTrue(storedDocumentHalResourceCollection.equals(storedDocumentHalResourceCollection));
    }

    @Test
    void shouldTestHashCode() {
        assertNotNull(storedDocumentHalResourceCollection.hashCode());
    }

    @Test
    void shouldTestHashCodeValue() {
        int result = storedDocumentHalResourceCollection.hashCode();
        assertEquals(31, result);
    }


    @Test
    void shouldTestToString() {
        String result = storedDocumentHalResourceCollection.toString();
        assertNotNull(result);
    }
}
