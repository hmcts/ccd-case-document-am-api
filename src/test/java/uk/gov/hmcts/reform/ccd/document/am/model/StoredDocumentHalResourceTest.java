package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StoredDocumentHalResourceTest {


    private transient StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();

    @Test
    void shouldAddLinks() {
        storedDocumentHalResource.addLinks(UUID.fromString("41334a2b-79ce-44eb-9168-2d49a744be9c"));
        assertEquals(2, storedDocumentHalResource.getLinks().size());
    }

    @Test
    void shouldTestEnums() {
        StoredDocumentHalResource.ClassificationEnum myEnum = StoredDocumentHalResource.ClassificationEnum.fromValue("PUBLIC");
        assertEquals("PUBLIC", myEnum.toString());
        myEnum = StoredDocumentHalResource.ClassificationEnum.fromValue("PRIVATE");
        assertEquals("PRIVATE", myEnum.toString());
        myEnum = StoredDocumentHalResource.ClassificationEnum.fromValue("RESTRICTED");
        assertEquals("RESTRICTED", myEnum.toString());
        myEnum = StoredDocumentHalResource.ClassificationEnum.fromValue("RESTRICT");
        assertNull(myEnum);

    }

}
