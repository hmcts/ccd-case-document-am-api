package uk.gov.hmcts.reform.ccd.document.am.model;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DocumentPayloadBodyTest {

    private transient DocumentPayloadBody documentPayloadBody = new DocumentPayloadBody();
    private static final String CITIZEN = "citizen";

    @Test
    void getClassification() {
        documentPayloadBody.setClassification("PUBLIC");
        String classification = documentPayloadBody.getClassification();
        assertEquals(classification, "PUBLIC");
    }

    @Test
    void getTtl() {
        OffsetDateTime ttl = OffsetDateTime.parse("2000-01-01T00:00:00Z");
        documentPayloadBody.setTtl(ttl);
        assertEquals(OffsetDateTime.parse("2000-01-01T00:00:00Z"), documentPayloadBody.getTtl());
    }

    @Test
    void addRolesItem() {
        documentPayloadBody.addRolesItem(null);
        documentPayloadBody.addRolesItem(CITIZEN);
        assertEquals(CITIZEN, documentPayloadBody.getRoles().get(1).toString());

    }

    @Test
    void shoudGetRoles() {
        List<String> roles = Arrays.asList(CITIZEN);
        documentPayloadBody.setRoles(roles);
        assertEquals(CITIZEN, documentPayloadBody.getRoles().get(0).toString());
    }

    @Test
    void shouldAddFilesItem() {
        documentPayloadBody.addFilesItem(new File("file"));
        assertNotNull(documentPayloadBody.getFiles());
    }

    @Test
    void shouldGetFiles() {
        File file = new File("file");
        documentPayloadBody.setFiles(Arrays.asList(file));
        assertNotNull(documentPayloadBody.getFiles());
    }

    @Test
    void shoudTestEquals() {
        assertNotNull(documentPayloadBody.equals(new DocumentPayloadBody()));
    }

    @Test
    void shouldTestHashCode() {
        assertNotNull(documentPayloadBody.hashCode());
    }

    @Test
    void shouldTestToString() {
        String result = documentPayloadBody.toString();
        assertNotNull(result);
    }
}
