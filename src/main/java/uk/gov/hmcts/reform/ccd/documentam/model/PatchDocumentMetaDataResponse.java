package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
public class PatchDocumentMetaDataResponse {
    @JsonProperty("Result")
    private @NotBlank String result;
}
