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


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StoredDocumentHalResourceCollection storedDocumentHalResourceCollection = (StoredDocumentHalResourceCollection) o;
        return Objects.equals(this.content, storedDocumentHalResourceCollection.content);

    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class StoredDocumentHalResourceCollection {\n");

        sb.append("    content: ").append(toIndentedString(content)).append("\n");
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
