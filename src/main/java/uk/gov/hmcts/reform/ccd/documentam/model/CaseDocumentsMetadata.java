package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_ID_MISSING;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_ID_NOT_VALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID_MISSING;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.INPUT_CASE_ID_PATTERN;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.INPUT_STRING_PATTERN;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID_MISSING;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseDocumentsMetadata {

    @JsonProperty
    @NotNull(message = CASE_ID_MISSING)
    @Size(min = 16, max = 16, message = CASE_ID_NOT_VALID)
    @Pattern(regexp = INPUT_CASE_ID_PATTERN, message = CASE_ID_NOT_VALID)
    private String caseId;

    @JsonProperty
    @NotNull(message = CASE_TYPE_ID_MISSING)
    @Pattern(regexp = INPUT_STRING_PATTERN, message = CASE_TYPE_ID_INVALID)
    private String caseTypeId;

    @JsonProperty
    @NotNull(message = JURISDICTION_ID_MISSING)
    @Pattern(regexp = INPUT_STRING_PATTERN, message = JURISDICTION_ID_INVALID)
    private String jurisdictionId;

    @JsonProperty("documentHashTokens")
    @Size(min = 1, message = "At least one document should be provided")
    private List<DocumentHashToken> documentHashTokens;

}
