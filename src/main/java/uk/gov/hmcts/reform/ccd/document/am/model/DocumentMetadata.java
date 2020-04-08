package uk.gov.hmcts.reform.ccd.document.am.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CaseDocumentMetadata.
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentMetadata {

    @JsonProperty
    private String caseId;

    @JsonProperty
    private String caseTypeId;

    @JsonProperty
    private String jurisdictionId;

    @JsonProperty("caseDocuments")
    private List<Document> documents;

}
