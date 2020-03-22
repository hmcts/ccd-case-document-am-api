package uk.gov.hmcts.reform.ccd.document.am.service;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;

public interface DocumentManagementService {

    ResponseEntity getDocumentMetadata(final UUID documentId);

    String extractCaseIdFromMetadata(Object storedDocument);

    ResponseEntity<Object> getDocumentBinaryContent(final UUID documentId);

    ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification, List<String> roles,
                                           String serviceAuthorization, String caseTypeId,
                                           String jurisdictionId, String userId);

    boolean checkUserPermission(ResponseEntity responseEntity, UUID documentId, String authorization, Permission permissionToCheck);

    ResponseEntity<Object> deleteDocument(final UUID documentId, String userId, String userRoles, Boolean permanent);

    boolean patchDocumentMetadata(DocumentMetadata caseDocumentMetadata,
                                  String serviceAuthorization, String userId);
}
