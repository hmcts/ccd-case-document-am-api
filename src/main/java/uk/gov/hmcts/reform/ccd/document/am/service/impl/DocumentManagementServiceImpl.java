package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadDocumentsCommand;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;

import java.util.Optional;
import java.util.UUID;

public class DocumentManagementServiceImpl implements DocumentManagementService {
    @Override
    public Optional<StoredDocumentHalResource> getDocumentMetadata(UUID documentId) {
        return Optional.empty();
    }

    @Override
    public String extractDocumentMetadata(StoredDocumentHalResource storedDocument) {
        return null;
    }

    @Override
    public ResponseEntity<?> getDocumentBinaryContent(UUID documentId) {
        return null;
    }

    @Override
    public StoredDocumentHalResourceCollection uploadDocumentsContent(UploadDocumentsCommand uploadDocumentsContent) {
        return null;
    }
}
