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
    private String id = null;

    @JsonProperty("url")
    private String url = null;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("type")
    private String type = null;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("hashToken")
    private String hashToken = null;

    @JsonProperty("permissions")
    private List<Permission> permissions = null;
}
