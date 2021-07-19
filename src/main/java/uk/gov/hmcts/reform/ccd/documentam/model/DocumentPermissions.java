package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.List;
import java.util.UUID;

@Validated
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentPermissions {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("permissions")
    private List<Permission> permissions;
}
