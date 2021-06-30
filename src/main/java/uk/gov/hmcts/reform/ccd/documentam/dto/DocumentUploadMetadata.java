package uk.gov.hmcts.reform.ccd.documentam.dto;

import lombok.Data;
import uk.gov.hmcts.reform.ccd.documentam.dto.validation.EnumValue;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.INPUT_STRING_PATTERN;

@Data
public class DocumentUploadMetadata {
    @NotNull(message = "Please provide classification")
    @Pattern(regexp = INPUT_STRING_PATTERN, message = "The input parameter does not comply with the required pattern")
    @EnumValue(enumClass = Classification.class)
    private final String classification;

    @NotNull(message = "Provide the Case Type ID ")
    @Pattern(regexp = INPUT_STRING_PATTERN, message = "The input parameter does not comply with the required pattern")
    private final String caseTypeId;

    @NotNull(message = "Provide the Jurisdiction ID")
    @Pattern(regexp = INPUT_STRING_PATTERN, message = "The input parameter does not comply with the required pattern")
    private final String jurisdictionId;
}
