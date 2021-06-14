package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.reform.ccd.documentam.controller.endpoints.CaseDocumentAmController;

import javax.validation.Valid;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Validated
@JsonIgnoreProperties(value = {"_links,_embedded,roles"})
public class StoredDocumentHalResource extends RepresentationModel<StoredDocumentHalResource> {

    @JsonProperty("classification")
    private ClassificationEnum classification = ClassificationEnum.PRIVATE;
    @JsonProperty("createdBy")
    private String createdBy;
    @JsonProperty("createdOn")
    private Date createdOn;
    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;
    @JsonProperty("metadata")
    @Valid
    private Map<String, String> metadata;
    @JsonProperty("mimeType")
    private String mimeType;
    @JsonProperty("modifiedOn")
    private Date modifiedOn;
    @JsonProperty("originalDocumentName")
    private String originalDocumentName;
    @JsonProperty("size")
    private Long size;
    @JsonProperty("ttl")
    private Date ttl;
    private String hashCode;

    public void addLinks(UUID documentId) {
        add(WebMvcLinkBuilder.linkTo(methodOn(
            CaseDocumentAmController.class).getDocumentByDocumentId(documentId, null)).withSelfRel());
        add(linkTo(methodOn(
            CaseDocumentAmController.class).getDocumentBinaryContentByDocumentId(documentId, null)).withRel("binary"));
    }

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
