package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.List;

@Validated
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentPermissions {

    @JsonProperty("id")
    private String id;

    @JsonProperty("permissions")
    private List<Permission> permissions;
}
