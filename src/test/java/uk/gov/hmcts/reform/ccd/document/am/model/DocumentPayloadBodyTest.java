package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentPayloadBodyTest {

    private transient DocumentPayloadBody documentPayloadBody = new DocumentPayloadBody();
    private static final String CITIZEN = "citizen";
    private transient DocumentPayloadBody documentPayloadBodyTest = new DocumentPayloadBody();

    @Test
    void getClassification() {
        documentPayloadBody.setClassification("PUBLIC");
        String classification = documentPayloadBody.getClassification();
        assertEquals("PUBLIC", classification);
        documentPayloadBodyTest.setClassification("PUBLIC");
    }

    @Test
    void getTtl() {
        OffsetDateTime ttl = OffsetDateTime.parse("2000-01-01T00:00:00Z");
        documentPayloadBody.setTtl(ttl);
        assertEquals(OffsetDateTime.parse("2000-01-01T00:00:00Z"), documentPayloadBody.getTtl());
    }

    @Test
    void addRolesItem() {
        documentPayloadBody.addRole(null);
        DocumentPayloadBody payloadBody = documentPayloadBody.addRole(CITIZEN);
        assertEquals(CITIZEN, documentPayloadBody.getRoles().get(1).toString());
        assertEquals(payloadBody.getClass(), DocumentPayloadBody.class);
    }

    @Test
    void shoudGetRoles() {
        List<String> roles = Arrays.asList(CITIZEN);
        documentPayloadBody.setRoles(roles);
        assertEquals(CITIZEN, documentPayloadBody.getRoles().get(0).toString());
    }

    @Test
    void shouldAddFilesItem() {
        DocumentPayloadBody payloadBody = documentPayloadBody.addFilesItem(new File("file"));
        assertNotNull(payloadBody);
        assertEquals(payloadBody.getClass(), DocumentPayloadBody.class);
    }

    @Test
    void shouldGetFiles() {
        File file = new File("file");
        documentPayloadBody.setFiles(Arrays.asList(file));
        assertNotNull(documentPayloadBody.getFiles());
    }

    @Test
    void shoudTestEquals() {
        boolean result = documentPayloadBody.equals(new DocumentPayloadBody());
        assertTrue(result);
    }

    @Test
    void assertEqualsTrue() {
        assertTrue(documentPayloadBody.equals(documentPayloadBody));
    }

    @Test
    void shoudTestEqualsNull() {
        assertFalse(documentPayloadBody.equals("string"));
    }

    @Test
    void shouldTestHashCode() {
        documentPayloadBody.setClassification("PUBLIC");
        File file = new File("file");
        documentPayloadBody.setFiles(Arrays.asList(file));
        OffsetDateTime ttl = OffsetDateTime.parse("2000-01-01T00:00:00Z");
        documentPayloadBody.setTtl(ttl);
        int result = documentPayloadBody.hashCode();
        assertEquals(-415235707, result);
    }

    @Test
    void shouldTestToString() {
        String result = documentPayloadBody.toString();
        assertNotNull(result);
    }
}
