package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;

import java.util.Date;
import java.util.Map;

@Builder(toBuilder = true)
@Jacksonized
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude
public class Document {

    private Classification classification;
    private Long size;
    private String mimeType;
    private String originalDocumentName;
    private String hashToken;
    private Date createdOn;
    private Date ttl;
    private Map<String, String> metadata;
    @JsonProperty("_links")
    private Links links;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        public Link self;
        public Link binary;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        public String href;
    }
}
