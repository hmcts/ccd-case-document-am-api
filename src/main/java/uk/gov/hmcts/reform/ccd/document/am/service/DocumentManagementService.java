package uk.gov.hmcts.reform.ccd.document.am.service;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;

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

    ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification, List<String> roles,
                                           String serviceAuthorization, String caseTypeId,
                                           String jurisdictionId, String userId);

    /**
     * Root GET endpoint.
     * @param responseEntity which has document meta data response
     * @param documentId Document Id for which binary content to be downloaded
     * @return Boolen object to check user permission
     **/
    boolean checkUserPermission(ResponseEntity responseEntity, UUID documentId, String authorization);

    /**
     * Root GET endpoint.
     * @param responseEntity which has document meta data response
     * @param documentId Document Id for which binary content to be downloaded
     * @return Boolen object to check user permission
     **/
    boolean patchDocumentMetadata(CaseDocumentMetadata body, String serviceAuthorization, String userId, String userRoles);
}
