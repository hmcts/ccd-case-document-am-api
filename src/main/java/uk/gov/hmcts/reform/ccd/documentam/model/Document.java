package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_JURISDICTION_ID;

@Builder(toBuilder = true)
@Jacksonized
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Document {

    Classification classification;
    Long size;
    String mimeType;
    String originalDocumentName;
    String hashToken;
    Date createdOn;
    String createdBy;
    String lastModifiedBy;
    Date modifiedOn;
    Date ttl;
    Map<String, String> metadata;
    @JsonProperty("_links")
    Links links;

    public  Map<String, String> getMetadata() {
        return Optional.ofNullable(metadata).orElse(Collections.emptyMap());
    }

    @JsonIgnore
    public String getCaseId() {
        return Optional.ofNullable(metadata)
            .map(metadataMap -> metadataMap.get(METADATA_CASE_ID))
            .orElse(null);
    }

    @JsonIgnore
    public String getCaseTypeId() {
        return Optional.ofNullable(metadata)
            .map(metadataMap -> metadataMap.get(METADATA_CASE_TYPE_ID))
            .orElse(null);
    }

    @JsonIgnore
    public String getJurisdictionId() {
        return Optional.ofNullable(metadata)
            .map(metadataMap -> metadataMap.get(METADATA_JURISDICTION_ID))
            .orElse(null);
    }

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
