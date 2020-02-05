package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Body
 */
@Validated
public class Body {
    @JsonProperty("classification")
    private String classification = null;

    @JsonProperty("ttl")
    private OffsetDateTime ttl = null;

    @JsonProperty("roles")
    @Valid
    private List<String> roles = null;

    @JsonProperty("files")
    @Valid
    private List<java.io.File> files = null;

    public Body classification(String classification) {
        this.classification = classification;
        return this;
    }

    /**
     * Get classification
     *
     * @return classification
     **/
    @ApiModelProperty(value = "")

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Body ttl(OffsetDateTime ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     * Get ttl
     *
     * @return ttl
     **/
    @ApiModelProperty(value = "")

    @Valid
    public OffsetDateTime getTtl() {
        return ttl;
    }

    public void setTtl(OffsetDateTime ttl) {
        this.ttl = ttl;
    }

    public Body roles(List<String> roles) {
        this.roles = roles;
        return this;
    }

    public Body addRolesItem(String rolesItem) {
        if (this.roles == null) {
            this.roles = new ArrayList<String>();
        }
        this.roles.add(rolesItem);
        return this;
    }

    /**
     * Get roles
     *
     * @return roles
     **/
    @ApiModelProperty(value = "")

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Body files(List<java.io.File> files) {
        this.files = files;
        return this;
    }

    public Body addFilesItem(java.io.File filesItem) {
        if (this.files == null) {
            this.files = new ArrayList<java.io.File>();
        }
        this.files.add(filesItem);
        return this;
    }

    /**
     * Get files
     *
     * @return files
     **/
    @ApiModelProperty(value = "")
    @Valid
    public List<java.io.File> getFiles() {
        return files;
    }

    public void setFiles(List<java.io.File> files) {
        this.files = files;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Body body = (Body) o;
        return Objects.equals(this.classification, body.classification) &&
            Objects.equals(this.ttl, body.ttl) &&
            Objects.equals(this.roles, body.roles) &&
            Objects.equals(this.files, body.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classification, ttl, roles, files);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Body {\n");

        sb.append("    classification: ").append(toIndentedString(classification)).append("\n");
        sb.append("    ttl: ").append(toIndentedString(ttl)).append("\n");
        sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
        sb.append("    files: ").append(toIndentedString(files)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
