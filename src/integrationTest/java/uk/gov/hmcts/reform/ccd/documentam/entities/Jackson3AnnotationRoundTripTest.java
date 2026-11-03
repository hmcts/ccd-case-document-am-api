package uk.gov.hmcts.reform.ccd.documentam.entities;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static java.time.ZoneOffset.UTC;

import java.util.Date;

class Jackson3AnnotationRoundTripTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final Date firstJan2030 = new Date(1893456000000L); // 1st Jan 2030

    @Test 
    void dm_ttl_request_uses_snake_case_and_ignores_unknown_properties() {
        DmTtlRequest request = new DmTtlRequest(ZonedDateTime.ofInstant(firstJan2030.toInstant(), UTC));
        String json = jsonMapper.writeValueAsString(request);
        assertThat(json).contains("\"ttl\":2030-01-01T00:00:00.000+00:00");

        DmTtlRequest deserializedRequest = jsonMapper.readValue("""
            {
                "ttl":"2030-01-01T00:00:00.000+00:00",
                "unknown_property": "ignored"
            }
            """, DmTtlRequest.class);
        assertThat(deserializedRequest).isEqualTo(request);
    }

    @Test
    void patch_document_response_uses_snake_case_and_ignores_unknown_properties() {
        String json = jsonMapper.writeValueAsString(new PatchDocumentResponse(
            firstJan2030,
            firstJan2030,
            firstJan2030,
            "original.pdf",
            "application/pdf", 
            "user"));

        assertThat(json).contains("\"ttl\":\"2030-01-01T00:00:00.000+00:00\"");
        assertThat(json).contains("\"created_on\":\"2030-01-01T00:00:00.000+00:00\"");
        assertThat(json).contains("\"modified_on\":\"2030-01-01T00:00:00.000+00:00\"");
        assertThat(json).contains("\"original_document_name\":\"original.pdf\"");
        assertThat(json).contains("\"mime_type\":\"application/pdf\"");
        assertThat(json).contains("\"last_modified_by\":\"user\"");
        assertThat(json).doesNotContain("createdOn");
        assertThat(json).doesNotContain("modifiedOn");
        assertThat(json).doesNotContain("originalDocumentName");
        assertThat(json).doesNotContain("mimeType");
        assertThat(json).doesNotContain("lastModifiedBy");

        PatchDocumentResponse response = jsonMapper.readValue("""
            {
                "ttl":null,
                "created_on":null,
                "modified_on":null,
                "original_document_name":null,
                "mime_type":null,
                "last_modified_by":null,
                "unknown_property": "ignored"
            }
            """,
            PatchDocumentResponse.class
        );

        assertThat(response).isEqualTo(new PatchDocumentResponse());
    }

}