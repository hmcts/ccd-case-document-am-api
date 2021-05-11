package uk.gov.hmcts.reform.ccd.documentam.auditlog;

public enum AuditOperationType {
    GET_DOCUMENT_BY_DOCUMENT_ID("GetDocumentByDocumentId"),
    PATCH_METADATA_ON_DOCUMENTS("PatchMetaDataOnDocuments"),
    GET_DOCUMENT_BINARY_CONTENT_BY_DOCUMENT_ID("GetDocumentBinaryContentByDocumentId"),
    UPLOAD_DOCUMENTS("UploadDocuments"),
    PATCH_DOCUMENT_BY_DOCUMENT_ID("PatchDocumentByDocumentId"),
    DELETE_DOCUMENT_BY_DOCUMENT_ID("DeleteDocumentByDocumentId"),
    GENERATE_HASH_CODE("GenerateHashCode");

    private final String label;

    AuditOperationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
