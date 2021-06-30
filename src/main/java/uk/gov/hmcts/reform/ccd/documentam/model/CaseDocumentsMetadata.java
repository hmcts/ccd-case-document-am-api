package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_ID_NOT_VALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.INPUT_CASE_ID_PATTERN;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseDocumentsMetadata {

    @JsonProperty
    @Size(min = 16, max = 16, message = CASE_ID_NOT_VALID)
    @Pattern(regexp = INPUT_CASE_ID_PATTERN, message = CASE_ID_NOT_VALID)
    private String caseId;

    @JsonProperty
    private String caseTypeId;

    @JsonProperty
    private String jurisdictionId;

    @JsonProperty("documentHashTokens")
    private List<DocumentHashToken> documentHashTokens;

}
