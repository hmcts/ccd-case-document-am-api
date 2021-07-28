package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmUploadResponse {

    @JsonProperty("_embedded")
    Embedded embedded;

    @Builder
    @Jacksonized
    @Value
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedded {
        List<Document> documents;
    }

}
