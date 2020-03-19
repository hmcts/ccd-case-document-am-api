package uk.gov.hmcts.reform.ccd.document.am.model;

import java.util.Map;
import java.util.UUID;
import javax.validation.constraints.NotBlank;

public class DocumentUpdate {

    private @NotBlank UUID documentId;
    private @NotBlank Map<String, String> metadata;


    public UUID getDocumentId() {
        return documentId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
