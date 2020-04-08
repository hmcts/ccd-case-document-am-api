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

    ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification,
                                            String caseTypeId,
                                           String jurisdictionId);

    boolean checkUserPermission(ResponseEntity responseEntity, UUID documentId, Permission permissionToCheck);

    ResponseEntity<Object> deleteDocument(final UUID documentId,  Boolean permanent);

    ResponseEntity patchDocument(final UUID documentId, UpdateDocumentCommand updateDocumentCommand);

    boolean patchDocumentMetadata(DocumentMetadata caseDocumentMetadata);
}
