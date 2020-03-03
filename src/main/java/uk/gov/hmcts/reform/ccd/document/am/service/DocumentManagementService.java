package uk.gov.hmcts.reform.ccd.document.am.service;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadDocumentsCommand;

import java.util.UUID;

public interface DocumentManagementService {

    /**
     * Root GET endpoint.
     *
     * @param documentId Document Id
     * @return Optional containing document details including metadata when found; empty optional otherwise
     */
    ResponseEntity getDocumentMetadata(final UUID documentId);

    /**
     * Root GET endpoint.
     *
     * @param storedDocument This is the storedDocument response object returned by DM-store
     * @return String containing case id extracted from document metadata
     **/
    String extractCaseIdFromMetadata(Object storedDocument);

    /**
     * Root GET endpoint.
     *
     * @param documentId Document Id for which binary content to be downloaded
     * @return OutputStream object containing binary content of document
     **/
    ResponseEntity<Object> getDocumentBinaryContent(final UUID documentId);

    /**
     * Root GET endpoint.
     *
     * @param uploadDocumentsContent The uploaded document content sent by service UI
     * @return StoredDocumentHalResourceCollection object containing stored document details
     */
    StoredDocumentHalResourceCollection uploadDocumentsContent(UploadDocumentsCommand uploadDocumentsContent);
}
