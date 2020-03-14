package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * CaseDocumentMetadata.
 */

@Data
@Builder
public class CaseDocumentMetadata {
    public CaseDocumentMetadata() {
    }

    public CaseDocumentMetadata(String caseId, String caseTypeId, String jurisdictionId, Document document) {
        this.caseId = caseId;
        this.caseTypeId = caseTypeId;
        this.jurisdictionId = jurisdictionId;
        this.document = document;
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
