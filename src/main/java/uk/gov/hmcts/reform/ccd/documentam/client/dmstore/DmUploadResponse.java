package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;

import java.util.List;

@Builder
@Jacksonized
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmUploadResponse {

    @JsonProperty("_embedded")
    private Embedded embedded;

    @Builder
    @Jacksonized
    @Value
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedded {
        private List<Document> documents;
    }

}
