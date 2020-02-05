package uk.gov.hmcts.reform.ccd.document.am.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

/**
 * CaseDocumentMetadata.
 */
@Validated

public class CaseDocumentMetadata {
    @JsonProperty("caseId")
    private String caseId = null;

    @JsonProperty("caseTypeId")
    private String caseTypeId = null;

    @JsonProperty("jurisdictionId")
    private String jurisdictionId = null;

    @JsonProperty("documents")
    @Valid
    private List<Document> documents = new ArrayList<Document>();

    public CaseDocumentMetadata caseId(String caseId) {
        this.caseId = caseId;
        return this;
    }

    /**
     * The CaseId of the Document.
     *
     * @return caseId
     **/
    @ApiModelProperty(required = true, value = "The CaseId of the Document")
    @NotNull

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public CaseDocumentMetadata caseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
        return this;
    }

    /**
     * The CaseTypeId of the Document.
     *
     * @return caseTypeId
     **/
    @ApiModelProperty(required = true, value = "The CaseTypeId of the Document")
    @NotNull

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public CaseDocumentMetadata jurisdictionId(String jurisdictionId) {
        this.jurisdictionId = jurisdictionId;
        return this;
    }

    /**
     * The JurisdictionId of the Document.
     *
     * @return jurisdictionId
     **/
    @ApiModelProperty(required = true, value = "The JurisdictionId of the Document")
    @NotNull

    public String getJurisdictionId() {
        return jurisdictionId;
    }

    public void setJurisdictionId(String jurisdictionId) {
        this.jurisdictionId = jurisdictionId;
    }

    public CaseDocumentMetadata documents(List<Document> documents) {
        this.documents = documents;
        return this;
    }

    public CaseDocumentMetadata addDocumentsItem(Document documentsItem) {
        this.documents.add(documentsItem);
        return this;
    }

    /**
     * List of embedded document objects.
     *
     * @return documents
     **/
    @ApiModelProperty(required = true, value = "List of embedded document objects")
    @NotNull
    @Valid
    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CaseDocumentMetadata caseDocumentMetadata = (CaseDocumentMetadata) o;
        return Objects.equals(this.caseId, caseDocumentMetadata.caseId)
               && Objects.equals(this.caseTypeId, caseDocumentMetadata.caseTypeId)
               && Objects.equals(this.jurisdictionId, caseDocumentMetadata.jurisdictionId)
               && Objects.equals(this.documents, caseDocumentMetadata.documents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, caseTypeId, jurisdictionId, documents);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CaseDocumentMetadata {\n");

        sb.append("    caseId: ").append(toIndentedString(caseId)).append("\n");
        sb.append("    caseTypeId: ").append(toIndentedString(caseTypeId)).append("\n");
        sb.append("    jurisdictionId: ").append(toIndentedString(jurisdictionId)).append("\n");
        sb.append("    documents: ").append(toIndentedString(documents)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces.
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
