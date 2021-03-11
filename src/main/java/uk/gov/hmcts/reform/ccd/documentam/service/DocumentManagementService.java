package uk.gov.hmcts.reform.ccd.documentam.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.List;
import java.util.UUID;

public interface DocumentManagementService {

    ResponseEntity<StoredDocumentHalResource> getDocumentMetadata(final UUID documentId);

    String extractCaseIdFromMetadata(StoredDocumentHalResource storedDocument);

    ResponseEntity<ByteArrayResource> getDocumentBinaryContent(final UUID documentId);

    ResponseEntity<HttpStatus> patchDocumentMetadata(CaseDocumentsMetadata caseDocumentsMetadata);

    String generateHashToken(final UUID documentId);

    ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification,
                                            String caseTypeId,
                                           String jurisdictionId);

    ResponseEntity<PatchDocumentResponse> patchDocument(final UUID documentId,
                                                        UpdateDocumentCommand updateDocumentCommand);

    ResponseEntity<HttpStatus> deleteDocument(final UUID documentId,  Boolean permanent);


    boolean checkUserPermission(ResponseEntity<StoredDocumentHalResource> responseEntity,
                                UUID documentId, Permission permissionToCheck);

    boolean checkServicePermission(ResponseEntity<StoredDocumentHalResource> documentMetadata,
                                   String serviceId, Permission permission);

    boolean checkServicePermissionsForUpload(String caseTypeId, String jurisdictionId,
                                             String serviceId, Permission create);

}
