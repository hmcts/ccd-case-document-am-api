package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.ArrayList;
import java.util.List;

@Validated
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorisedService {

    @JsonProperty("id")
    private String id;

    @JsonProperty("caseTypeId")
    private List<String> caseTypeId;

    @JsonProperty("jurisdictionId")
    private String jurisdictionId;

    @JsonProperty("defaultJurisdictionForTokenGeneration")
    private String defaultJurisdictionForTokenGeneration;

    @JsonProperty("defaultCaseTypeForTokenGeneration")
    private String defaultCaseTypeForTokenGeneration;

    @JsonProperty("permissions")
    private List<Permission> permissions;

    @Builder.Default
    @JsonProperty("caseTypeIdOptionalFor")
    private List<Permission> caseTypeIdOptionalFor = new ArrayList<>();

    @Builder.Default
    @JsonProperty("jurisdictionIdOptionalFor")
    private List<Permission> jurisdictionIdOptionalFor = new ArrayList<>();
}
