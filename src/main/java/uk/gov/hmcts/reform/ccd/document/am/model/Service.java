package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;

import java.util.List;

/**
 * Case Document.
 */
@Validated
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Service {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("caseTypeId")
    private String caseTypeId;

    @JsonProperty("jurisdictionId")
    private String jurisdictionId;

    @JsonProperty("permission")
    private List<Permission> permission;

}
