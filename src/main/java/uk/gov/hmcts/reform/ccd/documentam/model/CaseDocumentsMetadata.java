package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CaseDocumentsMetadata.
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseDocumentsMetadata {

    @JsonProperty
    private String caseId;

    @JsonProperty
    private String caseTypeId;

    @JsonProperty
    private String jurisdictionId;

    @JsonProperty("documentHashTokens")
    private List<DocumentHashToken> documentHashTokens;

}
