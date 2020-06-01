package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

/**
 * Case Document.
 */
@Validated
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentHashToken {

    @JsonProperty("id")
    private String id;

    @JsonProperty("hashToken")
    private String hashToken;

}
