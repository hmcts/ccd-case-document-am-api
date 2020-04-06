package uk.gov.hmcts.reform.ccd.document.am.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * CaseDocumentMetadata.
 */

@Data
@Builder
public class DocumentMetadata {
    public DocumentMetadata() {
    }

    public DocumentMetadata(String caseId, String caseTypeId, String jurisdictionId, List<Document> documents) {
        this.caseId = caseId;
        this.caseTypeId = caseTypeId;
        this.jurisdictionId = jurisdictionId;
        this.documents = documents;
    }

    @JsonProperty
    private String caseId;

    @JsonProperty
    private String caseTypeId;

    @JsonProperty
    private String jurisdictionId;

    @JsonProperty("caseDocuments")
    private List<Document> documents;

}
