package uk.gov.hmcts.reform.ccd.documentam.entities;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.util.StdDateFormat;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;

import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;

class Jackson3AnnotationRoundTripTest {

    private final JsonMapper jsonMapper = JsonMapper.builder()
        .changeDefaultPropertyInclusion(incl ->
                                            incl.withValueInclusion(JsonInclude.Include.NON_NULL))
        .defaultTimeZone(TimeZone.getTimeZone("UTC"))
        .defaultDateFormat(new StdDateFormat().withColonInTimeZone(true))
        .enable(DateTimeFeature.WRITE_UTC_AS_OFFSET)
        .build();

    private static final Date firstJan2030 = new Date(1893456000000L); // 1st Jan 2030

    @Test
    void patch_document_response_uses_ignores_unknown_properties() {
        String json = jsonMapper.writeValueAsString(new PatchDocumentResponse(
            firstJan2030,
            firstJan2030,
            firstJan2030,
            "original.pdf",
            "application/pdf", 
            "user"));

        assertThat(json).contains("\"ttl\":\"2030-01-01T00:00:00.000+00:00\"");
        assertThat(json).contains("\"createdOn\":\"2030-01-01T00:00:00.000+00:00\"");
        assertThat(json).contains("\"modifiedOn\":\"2030-01-01T00:00:00.000+00:00\"");
        assertThat(json).contains("\"originalDocumentName\":\"original.pdf\"");
        assertThat(json).contains("\"mimeType\":\"application/pdf\"");
        assertThat(json).contains("\"lastModifiedBy\":\"user\"");

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