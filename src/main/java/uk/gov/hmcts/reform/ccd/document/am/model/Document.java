package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;

import java.util.List;

/**
 * Document.
 */
@Validated
@Data
@Builder
public class Document {
    @JsonProperty("id")
    private String id;

    @JsonProperty("url")
    private String url;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("hashToken")
    private String hashToken;

    @JsonProperty("permissions")
    private List<Permission> permissions;
}
