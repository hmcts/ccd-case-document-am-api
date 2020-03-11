package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoredDocumentHalResourceTest {

    private transient StoredDocumentHalResource storedDocumentHalResource = new StoredDocumentHalResource();

    @Test
    void shouldAddLinks() {
        storedDocumentHalResource.addLinks(UUID.fromString("41334a2b-79ce-44eb-9168-2d49a744be9c"));
        assertEquals(2, storedDocumentHalResource.getLinks().size());
    }

}
