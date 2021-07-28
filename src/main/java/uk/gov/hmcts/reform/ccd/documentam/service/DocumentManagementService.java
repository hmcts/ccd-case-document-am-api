package uk.gov.hmcts.reform.ccd.documentam.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.UUID;

public interface DocumentManagementService {

    Document getDocumentMetadata(final UUID documentId);

    ResponseEntity<ByteArrayResource> getDocumentBinaryContent(final UUID documentId);

    void patchDocumentMetadata(CaseDocumentsMetadata caseDocumentsMetadata);

    String generateHashToken(UUID documentId, Document document);

    String generateHashToken(final UUID documentId);

    UploadResponse uploadDocuments(DocumentUploadRequest documentUploadRequest);

    PatchDocumentResponse patchDocument(UUID documentId, UpdateTtlRequest updateTtlRequest);

    void deleteDocument(final UUID documentId,  Boolean permanent);


    void checkUserPermission(String caseId,
                             UUID documentId,
                             Permission permissionToCheck,
                             String logMessage,
                             String exceptionMessage);

    void checkServicePermission(String caseTypeId,
                                String jurisdictionId,
                                String serviceId,
                                Permission permission,
                                String logMessage,
                                String exceptionMessage);

}
