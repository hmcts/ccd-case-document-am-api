package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    public Document(String id, String url, String name, String type, String description, String hashToken,
                    List<Permission> permissions) {
        this.id = id;
        this.url = url;
        this.name = name;
        this.type = type;
        this.description = description;
        this.hashToken = hashToken;
        this.permissions = permissions;
    }

    public Document() {
    }

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
