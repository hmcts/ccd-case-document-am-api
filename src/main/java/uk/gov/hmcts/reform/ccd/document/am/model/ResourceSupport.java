package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ResourceSupport.
 */
@Validated
public class ResourceSupport {
    @JsonProperty("links")
    @Valid
    private List<Link> links = null;

    public ResourceSupport links(List<Link> links) {
        this.links = links;
        return this;
    }

    public ResourceSupport addLinksItem(Link linksItem) {
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
        ResourceSupport resourceSupport = (ResourceSupport) o;
        return Objects.equals(this.links, resourceSupport.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(links);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ResourceSupport {\n");

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
