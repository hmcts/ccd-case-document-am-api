package uk.gov.hmcts.reform.ccd.document.am.service;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

import java.util.Optional;
import java.util.UUID;

public interface DocumentManagementService {

    /**
     * Root GET endpoint.
     *
     * @param documentId Document Id
     * @return Optional containing document details including metadata when found; empty optional otherwise
     */
    Optional<StoredDocumentHalResource> getDocumentMetadata(final UUID documentId);

    /**
     * Root GET endpoint.
     *
     * @param storedDocument This is the storedDocument response object returned by DM-store
     * @return String containing case id extracted from document metadata
     **/
    String extractDocumentMetadata(StoredDocumentHalResource storedDocument);

    /**
     * Root GET endpoint.
     *
     * @param documentId Document Id
     * @return OutputStream object containing binary content of document
     **/
    ResponseEntity<?> getDocumentBinaryContent(final UUID documentId);
}
