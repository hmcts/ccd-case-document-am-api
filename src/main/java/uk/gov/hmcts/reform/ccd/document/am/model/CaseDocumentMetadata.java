package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * CaseDocumentMetadata.
 */
@Validated
@Data
@Builder
public class CaseDocumentMetadata {
    @JsonProperty
    private String caseId;

    @JsonProperty
    private String caseTypeId;

    @JsonProperty
    private String jurisdictionId;

    @JsonProperty
    @Valid
    @Builder.Default
    private Optional<List<Document>> documents = Optional.empty();
}
