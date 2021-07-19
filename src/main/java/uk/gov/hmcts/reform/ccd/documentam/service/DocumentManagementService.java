package uk.gov.hmcts.reform.ccd.documentam.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentManagementService {

    Optional<StoredDocumentHalResource> getDocumentMetadata(final UUID documentId);

    String extractCaseIdFromMetadata(StoredDocumentHalResource storedDocument);

    ResponseEntity<ByteArrayResource> getDocumentBinaryContent(final UUID documentId);

    void patchDocumentMetadata(CaseDocumentsMetadata caseDocumentsMetadata);

    String generateHashToken(final UUID documentId, StoredDocumentHalResource documentMetadata);

    UploadResponse uploadDocuments(List<MultipartFile> files, String classification,
                                   String caseTypeId,
                                   String jurisdictionId);

    ResponseEntity<PatchDocumentResponse> patchDocument(final UUID documentId,
                                                        UpdateDocumentCommand updateDocumentCommand);

    void deleteDocument(final UUID documentId,  Boolean permanent);


    void checkUserPermission(StoredDocumentHalResource documentMetadata,
                             UUID documentId, Permission permissionToCheck,
                             String logMessage, String exceptionMessage);

    void checkServicePermission(StoredDocumentHalResource documentMetadata,
                                String serviceId, Permission permission,
                                String logMessage, String exceptionMessage);

    void checkServicePermission(String caseTypeId, String jurisdictionId,
                                String serviceId, Permission permission,
                                String logMessage, String exceptionMessage);

    void validateHashTokens(List<DocumentHashToken> documentList);
}
