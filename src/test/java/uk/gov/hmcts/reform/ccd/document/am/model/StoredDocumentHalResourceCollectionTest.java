package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
class StoredDocumentHalResourceCollectionTest {

    private transient StoredDocumentHalResourceCollection storedDocumentHalResourceCollection = new StoredDocumentHalResourceCollection();

    @Test
    void shouldAddContentItem() {
        List<String> roles = Arrays.asList("citizen");
        storedDocumentHalResourceCollection.addContentItem(new StoredDocumentHalResource());
        assertNotNull(storedDocumentHalResourceCollection.getContent());
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
    void shouldTestHashCode() {
        assertNotNull(storedDocumentHalResourceCollection.hashCode());
    }

    @Test
    void shouldTestToString() {
        String result = storedDocumentHalResourceCollection.toString();
        assertNotNull(result);
    }
}
