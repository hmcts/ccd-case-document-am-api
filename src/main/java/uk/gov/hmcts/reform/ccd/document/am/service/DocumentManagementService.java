package uk.gov.hmcts.reform.ccd.document.am.service;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;

public interface DocumentManagementService {

    ResponseEntity getDocumentMetadata(final UUID documentId);

    String extractCaseIdFromMetadata(Object storedDocument);

    ResponseEntity<Object> getDocumentBinaryContent(final UUID documentId);

    ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification, List<String> roles,
                                            String caseTypeId,
                                           String jurisdictionId, String userId);

    boolean checkUserPermission(ResponseEntity responseEntity, UUID documentId, Permission permissionToCheck);

    ResponseEntity<Object> deleteDocument(final UUID documentId, String userId, String userRoles, Boolean permanent);

    ResponseEntity patchDocument(final UUID documentId, UpdateDocumentCommand updateDocumentCommand,
                                             String userId, String userRoles);

    boolean patchDocumentMetadata(DocumentMetadata caseDocumentMetadata, String userId);
}
