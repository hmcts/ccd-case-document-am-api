package uk.gov.hmcts.reform.ccd.documentam.dto;

import io.swagger.annotations.ApiParam;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.dto.validation.ClassificationValue;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import java.util.List;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID_MISSING;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CLASSIFICATION_MISSING;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.INPUT_STRING_PATTERN;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID_MISSING;

@Data
public class DocumentUploadRequest {
    @ApiParam(value = "List of file to be uploaded", required = true)
    @NotNull(message = "Provide some file to be uploaded")
    @Size(min = 1, message = "Please provide at least one file to be uploaded.")
    private final List<MultipartFile> files;

    @ApiParam(value = "Security classification for the file", required = true)
    @NotNull(message = CLASSIFICATION_MISSING)
    @ClassificationValue
    private final String classification;

    @ApiParam(value = "CaseType identifier for the case document.", required = true)
    @NotNull(message = CASE_TYPE_ID_MISSING)
    @Pattern(regexp = INPUT_STRING_PATTERN, message = CASE_TYPE_ID_INVALID)
    private final String caseTypeId;

    @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
    @NotNull(message = JURISDICTION_ID_MISSING)
    @Pattern(regexp = INPUT_STRING_PATTERN, message = JURISDICTION_ID_INVALID)
    private final String jurisdictionId;
}
