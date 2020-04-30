package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CaseDocumentsMetadata.
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseDocumentMetadata {

    @JsonProperty
    private String caseId;

    @JsonProperty("documentPermissions")
    private DocumentPermissions documentPermissions;

}
