package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;

import java.util.Optional;

/**
 * CaseDocumentMetadata.
 */

@Data
public class CaseDocumentMetadata {
    public CaseDocumentMetadata() {
    }

    @JsonProperty
    private String caseId;

    @JsonProperty
    private String caseTypeId;

    @JsonProperty
    private String jurisdictionId;

    @JsonProperty
    private Document document;
}
