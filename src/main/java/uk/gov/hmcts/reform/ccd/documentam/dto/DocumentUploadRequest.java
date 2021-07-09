package uk.gov.hmcts.reform.ccd.documentam.dto;

import io.swagger.annotations.ApiParam;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.dto.validation.ClassificationValue;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import java.util.List;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.INPUT_STRING_PATTERN;

@Data
public class DocumentUploadRequest {
    @ApiParam(value = "List of file to be uploaded", required = true)
    @NotNull(message = "Provide some file to be uploaded")
    @Size(min = 1, message = "Please provide at least one file to be uploaded.")
    private final List<MultipartFile> files;

    @ApiParam(value = "Security classification for the file", required = true)
    @NotNull(message = "Please provide Classification")
    @ClassificationValue
    private final String classification;

    @ApiParam(value = "CaseType identifier for the case document.", required = true)
    @NotNull(message = "Provide the Case Type ID")
    @Pattern(regexp = INPUT_STRING_PATTERN, message = "The Case Type ID is not valid")
    private final String caseTypeId;

    @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
    @NotNull(message = "Provide the Jurisdiction ID")
    @Pattern(regexp = INPUT_STRING_PATTERN, message = "The Jurisdiction ID is not valid")
    private final String jurisdictionId;
}
