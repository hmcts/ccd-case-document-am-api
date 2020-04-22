package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * CaseDocumentsMetadata.
 */

@Validated
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Services {

    @JsonProperty("service")
    private List<Service> service;

}
