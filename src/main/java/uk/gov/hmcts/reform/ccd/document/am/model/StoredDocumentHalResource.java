package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * StoredDocumentHalResource
 */
@Validated
public class StoredDocumentHalResource {
    @JsonProperty("_embedded")
    @Valid
    private Map<String, ResourceSupport> _embedded = null;
    @JsonProperty("classification")
    private ClassificationEnum classification = ClassificationEnum.PRIVATE;
    @JsonProperty("createdBy")
    private String createdBy = null;
    @JsonProperty("createdOn")
    private Date createdOn = null;
    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy = null;
    @JsonProperty("links")
    @Valid
    private List<Link> links = null;
    @JsonProperty("metadata")
    @Valid
    private Map<String, String> metadata = null;
    @JsonProperty("mimeType")
    private String mimeType = null;
    @JsonProperty("modifiedOn")
    private Date modifiedOn = null;
    @JsonProperty("originalDocumentName")
    private String originalDocumentName = null;
    @JsonProperty("roles")
    @Valid
    private List<String> roles = null;
    @JsonProperty("size")
    private Long size = null;
    @JsonProperty("ttl")
    private Date ttl = null;

    public StoredDocumentHalResource _embedded(Map<String, ResourceSupport> _embedded) {
        this._embedded = _embedded;
        return this;
    }

    public StoredDocumentHalResource putEmbeddedItem(String key, ResourceSupport _embeddedItem) {
        if (this._embedded == null) {
            this._embedded = new HashMap<String, ResourceSupport>();
        }
        this._embedded.put(key, _embeddedItem);
        return this;
    }

    /**
     * Get _embedded
     *
     * @return _embedded
     **/
    @ApiModelProperty(value = "")
    @Valid
    public Map<String, ResourceSupport> getEmbedded() {
        return _embedded;
    }

    public void setEmbedded(Map<String, ResourceSupport> _embedded) {
        this._embedded = _embedded;
    }

    public StoredDocumentHalResource classification(ClassificationEnum classification) {
        this.classification = classification;
        return this;
    }

    /**
     * Get classification
     *
     * @return classification
     **/
    @ApiModelProperty(value = "")

    public ClassificationEnum getClassification() {
        return classification;
    }

    public void setClassification(ClassificationEnum classification) {
        this.classification = classification;
    }

    public StoredDocumentHalResource createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    /**
     * Get createdBy
     *
     * @return createdBy
     **/
    @ApiModelProperty(value = "")

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public StoredDocumentHalResource createdOn(Date createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    /**
     * Get createdOn
     *
     * @return createdOn
     **/
    @ApiModelProperty(value = "")

    @Valid
    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public StoredDocumentHalResource lastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
        return this;
    }

    /**
     * Get lastModifiedBy
     *
     * @return lastModifiedBy
     **/
    @ApiModelProperty(value = "")

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public StoredDocumentHalResource links(List<Link> links) {
        this.links = links;
        return this;
    }

    public StoredDocumentHalResource addLinksItem(Link linksItem) {
        if (this.links == null) {
            this.links = new ArrayList<Link>();
        }
        this.links.add(linksItem);
        return this;
    }

    /**
     * Get links
     *
     * @return links
     **/
    @ApiModelProperty(value = "")
    @Valid
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public StoredDocumentHalResource metadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public StoredDocumentHalResource putMetadataItem(String key, String metadataItem) {
        if (this.metadata == null) {
            this.metadata = new HashMap<String, String>();
        }
        this.metadata.put(key, metadataItem);
        return this;
    }

    /**
     * Get metadata
     *
     * @return metadata
     **/
    @ApiModelProperty(value = "")

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public StoredDocumentHalResource mimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    /**
     * Get mimeType
     *
     * @return mimeType
     **/
    @ApiModelProperty(value = "")

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public StoredDocumentHalResource modifiedOn(Date modifiedOn) {
        this.modifiedOn = modifiedOn;
        return this;
    }

    /**
     * Get modifiedOn
     *
     * @return modifiedOn
     **/
    @ApiModelProperty(value = "")

    @Valid
    public Date getModifiedOn() {
        return modifiedOn;
    }

    public void setModifiedOn(Date modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    public StoredDocumentHalResource originalDocumentName(String originalDocumentName) {
        this.originalDocumentName = originalDocumentName;
        return this;
    }

    /**
     * Get originalDocumentName
     *
     * @return originalDocumentName
     **/
    @ApiModelProperty(value = "")

    public String getOriginalDocumentName() {
        return originalDocumentName;
    }

    public void setOriginalDocumentName(String originalDocumentName) {
        this.originalDocumentName = originalDocumentName;
    }

    public StoredDocumentHalResource roles(List<String> roles) {
        this.roles = roles;
        return this;
    }

    public StoredDocumentHalResource addRolesItem(String rolesItem) {
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

    public StoredDocumentHalResource size(Long size) {
        this.size = size;
        return this;
    }

    /**
     * Get size
     *
     * @return size
     **/
    @ApiModelProperty(value = "")

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public StoredDocumentHalResource ttl(Date ttl) {
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
    public Date getTtl() {
        return ttl;
    }

    public void setTtl(Date ttl) {
        this.ttl = ttl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StoredDocumentHalResource storedDocumentHalResource = (StoredDocumentHalResource) o;
        return Objects.equals(this._embedded, storedDocumentHalResource._embedded) &&
            Objects.equals(this.classification, storedDocumentHalResource.classification) &&
            Objects.equals(this.createdBy, storedDocumentHalResource.createdBy) &&
            Objects.equals(this.createdOn, storedDocumentHalResource.createdOn) &&
            Objects.equals(this.lastModifiedBy, storedDocumentHalResource.lastModifiedBy) &&
            Objects.equals(this.links, storedDocumentHalResource.links) &&
            Objects.equals(this.metadata, storedDocumentHalResource.metadata) &&
            Objects.equals(this.mimeType, storedDocumentHalResource.mimeType) &&
            Objects.equals(this.modifiedOn, storedDocumentHalResource.modifiedOn) &&
            Objects.equals(this.originalDocumentName, storedDocumentHalResource.originalDocumentName) &&
            Objects.equals(this.roles, storedDocumentHalResource.roles) &&
            Objects.equals(this.size, storedDocumentHalResource.size) &&
            Objects.equals(this.ttl, storedDocumentHalResource.ttl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_embedded, classification, createdBy, createdOn, lastModifiedBy, links, metadata, mimeType, modifiedOn, originalDocumentName, roles, size, ttl);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class StoredDocumentHalResource {\n");

        sb.append("    _embedded: ").append(toIndentedString(_embedded)).append("\n");
        sb.append("    classification: ").append(toIndentedString(classification)).append("\n");
        sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
        sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
        sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
        sb.append("    links: ").append(toIndentedString(links)).append("\n");
        sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
        sb.append("    mimeType: ").append(toIndentedString(mimeType)).append("\n");
        sb.append("    modifiedOn: ").append(toIndentedString(modifiedOn)).append("\n");
        sb.append("    originalDocumentName: ").append(toIndentedString(originalDocumentName)).append("\n");
        sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
        sb.append("    size: ").append(toIndentedString(size)).append("\n");
        sb.append("    ttl: ").append(toIndentedString(ttl)).append("\n");
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

    /**
     * Gets or Sets classification
     */
    public enum ClassificationEnum {
        PUBLIC("PUBLIC"),

        PRIVATE("PRIVATE"),

        RESTRICTED("RESTRICTED");

        private String value;

        ClassificationEnum(String value) {
            this.value = value;
        }

        @JsonCreator
        public static ClassificationEnum fromValue(String text) {
            for (ClassificationEnum b : ClassificationEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }
}
