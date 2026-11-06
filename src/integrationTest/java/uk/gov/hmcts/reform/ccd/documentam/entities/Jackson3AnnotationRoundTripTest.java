package uk.gov.hmcts.reform.ccd.documentam.entities;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.util.StdDateFormat;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse.Embedded;

import java.util.Date;
import java.util.List;
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
    void dm_document_upload_uses_ignores_unknown_properties() {
        String json = jsonMapper.writeValueAsString(DmUploadResponse.builder().embedded(
            Embedded.builder().documents(List.of(
                Document.builder()
                    .originalDocumentName("original.pdf")
                    .mimeType("application/pdf")
                    .createdOn(firstJan2030)
                    .createdBy("created-test")
                    .modifiedOn(firstJan2030)
                    .lastModifiedBy("modified-test")
                    .build()
            )).build()
        ).build());

        assertThat(json).contains("\"originalDocumentName\":\"original.pdf\"");
        assertThat(json).contains("\"mimeType\":\"application/pdf\"");
        assertThat(json).contains("\"createdOn\":\"2030-01-01T00:00:00.000+00:00\"");
        assertThat(json).contains("\"createdBy\":\"created-test\"");
        assertThat(json).contains("\"modifiedOn\":\"2030-01-01T00:00:00.000+00:00\"");
        assertThat(json).contains("\"lastModifiedBy\":\"modified-test\"");

        DmUploadResponse response = jsonMapper.readValue("""
            {
                "_embedded": {
                    "documents": [
                        {
                            "originalDocumentName": "original.pdf",
                            "mimeType": "application/pdf",
                            "createdOn": "2030-01-01T00:00:00.000+00:00",
                            "createdBy": "created-test",
                            "modifiedOn": "2030-01-01T00:00:00.000+00:00",
                            "lastModifiedBy": "modified-test"
                        }
                    ]
                }
            }
            """, DmUploadResponse.class);
        assertThat(response.getEmbedded().getDocuments().get(0).getOriginalDocumentName()).isEqualTo("original.pdf");
        assertThat(response.getEmbedded().getDocuments().get(0).getMimeType()).isEqualTo("application/pdf");
        assertThat(response.getEmbedded().getDocuments().get(0).getCreatedOn()).isEqualTo(firstJan2030);
        assertThat(response.getEmbedded().getDocuments().get(0).getCreatedBy()).isEqualTo("created-test");
        assertThat(response.getEmbedded().getDocuments().get(0).getModifiedOn()).isEqualTo(firstJan2030);
        assertThat(response.getEmbedded().getDocuments().get(0).getLastModifiedBy()).isEqualTo("modified-test");
    }

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
                "ttl":"2030-01-01T00:00:00.000+00:00",
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

        assertThat(response).isEqualTo(new PatchDocumentResponse(firstJan2030, null, null, null, null, null));
    }

}