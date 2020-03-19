package uk.gov.hmcts.reform.ccd.document.am.service;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;

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
     * Root PATCH endpoint.
     *
     * @param documentId Document Id
     * @return updateDocumentCommand UpdateDocumentCommand
     */
    ResponseEntity patchDocumentbyDocumentId(final UUID documentId, UpdateDocumentCommand updateDocumentCommand);

    /**
     * Root GET endpoint.
     * @param responseEntity which has document meta data response
     * @param documentId Document Id for which binary content to be downloaded
     * @return Boolen object to check user permission
     **/
    boolean checkUserPermission(ResponseEntity responseEntity, UUID documentId, String authorization, Permission permissionToCheck);
}
