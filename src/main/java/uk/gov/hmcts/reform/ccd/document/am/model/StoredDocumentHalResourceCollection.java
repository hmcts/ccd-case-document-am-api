package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * StoredDocumentHalResourceCollection.
 */
@Validated
public class StoredDocumentHalResourceCollection {
    @JsonProperty("content")
    @Valid
    private List<StoredDocumentHalResource> content = null;

    @JsonProperty("links")
    @Valid
    private List<Link> links = null;


    public StoredDocumentHalResourceCollection addContentItem(StoredDocumentHalResource contentItem) {
        if (this.content == null) {
            this.content = new ArrayList<StoredDocumentHalResource>();
        }
        this.content.add(contentItem);
        return this;
    }

    /**
     * The list of StoredDocumentHalResource object.
     *
     * @return content
     **/
    @ApiModelProperty(value = "The list of StoredDocumentHalResource object.")
    @Valid
    public List<StoredDocumentHalResource> getContent() {
        return content;
    }

    public void setContent(List<StoredDocumentHalResource> content) {
        this.content = content;
    }

    public StoredDocumentHalResourceCollection addLinksItem(Link linksItem) {
        if (this.links == null) {
            this.links = new ArrayList<Link>();
        }
        this.links.add(linksItem);
        return this;
    }

    /**
     * Get links.
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


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StoredDocumentHalResourceCollection storedDocumentHalResourceCollection = (StoredDocumentHalResourceCollection) o;
        return Objects.equals(this.content, storedDocumentHalResourceCollection.content)
               && Objects.equals(this.links, storedDocumentHalResourceCollection.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, links);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class StoredDocumentHalResourceCollection {\n");

        sb.append("    content: ").append(toIndentedString(content)).append("\n");
        sb.append("    links: ").append(toIndentedString(links)).append("\n");
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
