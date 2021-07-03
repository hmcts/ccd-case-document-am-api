package uk.gov.hmcts.reform.ccd.documentam.model;

import java.util.List;

public class UploadResponse {

    private List<Document> documents;

    public UploadResponse(List<Document> documents) {
        this.documents = documents;
    }

    public List<Document> getDocuments() {
        return documents;
    }
}
