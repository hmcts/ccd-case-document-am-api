package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.Objects;

/**
 * Link.
 */
@Validated
public class Link {
    @JsonIgnore
    private String deprecation = null;

    @JsonIgnore
    private String href = null;

    @JsonProperty("self")
    private Map<String,String> self;

    @JsonProperty("binary")
    private Map<String,String> binary;

    @JsonProperty("thumbnail")
    private Map<String,String> thumbnail;

    @JsonIgnore
    private String hreflang = null;

    @JsonIgnore
    private String media = null;

    @JsonIgnore
    private String rel = null;

    @JsonIgnore
    private transient boolean templated;

    @JsonIgnore
    private String title = null;

    @JsonIgnore
    private String type = null;


    /**
     * Get deprecation.
     *
     * @return deprecation
     **/
    @ApiModelProperty(value = "")

    public String getDeprecation() {
        return deprecation;
    }

    public void setDeprecation(String deprecation) {
        this.deprecation = deprecation;
    }

    /**
     * Get href.
     *
     * @return href
     **/
    @ApiModelProperty(value = "")

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    /**
     * Get hreflang.
     *
     * @return hreflang
     **/
    @ApiModelProperty(value = "")

    public String getHreflang() {
        return hreflang;
    }

    public void setHreflang(String hreflang) {
        this.hreflang = hreflang;
    }

    /**
     * Get media.
     *
     * @return media
     **/
    @ApiModelProperty(value = "")

    public String getMedia() {
        return media;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    /**
     * Get rel.
     *
     * @return rel
     **/
    @ApiModelProperty(value = "")

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }


    /**
     * Get templated.
     *
     * @return templated
     **/
    @ApiModelProperty(value = "")

    public Boolean isTemplated() {
        return templated;
    }

    public void setTemplated(Boolean templated) {
        this.templated = templated;
    }

    /**
     * Get title.
     *
     * @return title
     **/
    @ApiModelProperty(value = "")

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get type.
     *
     * @return type
     **/
    @ApiModelProperty(value = "")

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getSelf() {
        return self;
    }

    public Map<String, String> getBinary() {
        return binary;
    }

    public void setSelf(Map<String, String> self) {
        this.self = self;
    }

    public void setBinary(Map<String, String> binary) {
        this.binary = binary;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Link link = (Link) o;
        return Objects.equals(this.deprecation, link.deprecation)
               && Objects.equals(this.href, link.href)
               && Objects.equals(this.hreflang, link.hreflang)
               && Objects.equals(this.media, link.media)
               && Objects.equals(this.rel, link.rel)
               && Objects.equals(this.templated, link.templated)
               && Objects.equals(this.title, link.title)
               && Objects.equals(this.type, link.type)
               && Objects.equals(this.self, link.self)
               && Objects.equals(this.binary, link.binary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deprecation, href, hreflang, media, rel, templated, title, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Link {\n");

        sb.append("    deprecation: ").append(toIndentedString(deprecation)).append("\n");
        sb.append("    href: ").append(toIndentedString(href)).append("\n");
        sb.append("    hreflang: ").append(toIndentedString(hreflang)).append("\n");
        sb.append("    media: ").append(toIndentedString(media)).append("\n");
        sb.append("    rel: ").append(toIndentedString(rel)).append("\n");
        sb.append("    templated: ").append(toIndentedString(templated)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    self: ").append(toIndentedString(self)).append("\n");
        sb.append("    binary: ").append(toIndentedString(binary)).append("\n");
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
