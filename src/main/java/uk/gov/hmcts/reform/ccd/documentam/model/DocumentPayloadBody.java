package uk.gov.hmcts.reform.ccd.documentam.model;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

@Validated
public class DocumentPayloadBody {
    @JsonProperty("classification")
    private String classification;

    @JsonProperty("ttl")
    private OffsetDateTime ttl;

    @JsonProperty("roles")
    @Valid
    private List<String> roles;

    @JsonProperty("files")
    @Valid
    private List<File> files;

    @ApiModelProperty
    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    @ApiModelProperty
    @Valid
    public OffsetDateTime getTtl() {
        return ttl;
    }

    public void setTtl(OffsetDateTime ttl) {
        this.ttl = ttl;
    }

    public DocumentPayloadBody addRole(String rolesItem) {
        if (this.roles == null) {
            this.roles = new ArrayList<>();
        }
        this.roles.add(rolesItem);
        return this;
    }

    @ApiModelProperty
    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public DocumentPayloadBody addFilesItem(File filesItem) {
        if (this.files == null) {
            this.files = new ArrayList<>();
        }
        this.files.add(filesItem);
        return this;
    }

    @ApiModelProperty
    @Valid
    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
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
        DocumentPayloadBody documentPayloadBody = (DocumentPayloadBody) o;
        return Objects.equals(this.classification, documentPayloadBody.classification)
               && Objects.equals(this.ttl, documentPayloadBody.ttl)
               && Objects.equals(this.roles, documentPayloadBody.roles)
               && Objects.equals(this.files, documentPayloadBody.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classification, ttl, roles, files);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DocumentPayloadBody {\n");

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
