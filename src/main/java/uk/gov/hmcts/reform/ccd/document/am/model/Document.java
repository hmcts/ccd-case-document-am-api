package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Document.
 */
@Validated
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

    //put hashtoken
    @JsonProperty("hashToken")
    private String hashToken = null;

    /**
     * Unique ID for the document.
     *
     * @return id
     **/
    @ApiModelProperty(required = true, value = "Unique ID for the document")
    @NotNull

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * The document URL.
     *
     * @return url
     **/
    @ApiModelProperty(required = true, value = "The document URL")
    @NotNull

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * The document name.
     *
     * @return name
     **/
    @ApiModelProperty(value = "The document name")

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The document type.
     *
     * @return type
     **/
    @ApiModelProperty(value = "The document type")

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * The document description.
     *
     * @return description
     **/
    @ApiModelProperty(value = "The document description")

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The hashToken.
     *
     * @return hashToken
     **/
    @ApiModelProperty(value = "The hashToken")

    public String getHashToken() {
        return hashToken;
    }

    public void setHashToken(String hashToken) {
        this.hashToken = hashToken;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Document document = (Document) o;
        return Objects.equals(this.id, document.id)
               && Objects.equals(this.url, document.url)
               && Objects.equals(this.name, document.name)
               && Objects.equals(this.type, document.type)
               && Objects.equals(this.description, document.description)
               && Objects.equals(this.hashToken, document.hashToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, name, type, description, hashToken);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Document {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    url: ").append(toIndentedString(url)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    hashToken: ").append(toIndentedString(hashToken)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
