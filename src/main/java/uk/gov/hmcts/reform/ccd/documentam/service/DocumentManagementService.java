package uk.gov.hmcts.reform.ccd.documentam.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.List;
import java.util.UUID;

public interface DocumentManagementService {

    ResponseEntity<Object> getDocumentMetadata(final UUID documentId);

    String extractCaseIdFromMetadata(Object storedDocument);

    ResponseEntity<Object> getDocumentBinaryContent(final UUID documentId);

    ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification,
                                            String caseTypeId,
                                           String jurisdictionId);

    boolean checkUserPermission(ResponseEntity<?> responseEntity, UUID documentId, Permission permissionToCheck);

    ResponseEntity<Object> deleteDocument(final UUID documentId,  Boolean permanent);

    ResponseEntity<Object> patchDocument(final UUID documentId, UpdateDocumentCommand updateDocumentCommand);

    ResponseEntity<Object> patchDocumentMetadata(CaseDocumentsMetadata caseDocumentsMetadata);

    String generateHashToken(final UUID documentId);

    boolean checkServicePermission(ResponseEntity<?> documentMetadata, String serviceId, Permission permission);

    boolean checkServicePermissionsForUpload(String caseTypeId, String jurisdictionId,
                                             String serviceId, Permission create);

}
