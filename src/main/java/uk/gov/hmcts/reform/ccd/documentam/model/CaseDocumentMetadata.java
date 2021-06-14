package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
