package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DocumentStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentUpdate;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.client.datastore.CaseDataStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.documentam.util.ApplicationUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DOCUMENT_METADATA_NOT_FOUND;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_JURISDICTION_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.RESOURCE_NOT_FOUND;

@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private static final Date NULL_TTL = null;

    private final DocumentStoreClient documentStoreClient;

    private final CaseDataStoreClient caseDataStoreClient;

    private final AuthorisedServices authorisedServices;

    private final ApplicationParams applicationParams;

    @Autowired
    public DocumentManagementServiceImpl(final DocumentStoreClient documentStoreClient,
                                         final CaseDataStoreClient caseDataStoreClient,
                                         final AuthorisedServices authorisedServices,
                                         final ApplicationParams applicationParams) {
        this.documentStoreClient = documentStoreClient;
        this.caseDataStoreClient = caseDataStoreClient;
        this.authorisedServices = authorisedServices;
        this.applicationParams = applicationParams;
    }

    @Override
    public Document getDocumentMetadata(UUID documentId) {
        return documentStoreClient.getDocument(documentId)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(
                DOCUMENT_METADATA_NOT_FOUND,
                documentId.toString()
            )));
    }

    @Override
    public ResponseEntity<ByteArrayResource> getDocumentBinaryContent(UUID documentId) {
        return documentStoreClient.getDocumentAsBinary(documentId);
    }

    @Override
    public void patchDocumentMetadata(final CaseDocumentsMetadata caseDocumentsMetadata) {
        final UpdateDocumentsCommand updateDocumentsCommand
            = prepareRequestForAttachingDocumentToCase(caseDocumentsMetadata);

        documentStoreClient.patchDocumentMetadata(updateDocumentsCommand);
    }

    private UpdateDocumentsCommand prepareRequestForAttachingDocumentToCase(CaseDocumentsMetadata
                                                                                caseDocumentsMetadata) {
        List<DocumentUpdate> documentsList = new ArrayList<>();
        for (DocumentHashToken documentHashToken : caseDocumentsMetadata.getDocumentHashTokens()) {
            final Optional<Document> documentMetadata = documentStoreClient.getDocument(documentHashToken.getId());

            if (documentHashToken.getHashToken() != null) {
                if (documentMetadata.isEmpty()) {
                    throw new ResourceNotFoundException(String.format(
                        "Meta data does not exist for documentId: %s",
                        documentHashToken.getId()
                    ));
                }
                verifyHashTokenValidity(documentHashToken, documentMetadata.get());
            } else if (applicationParams.isHashCheckEnabled()) {
                throw new ForbiddenException(
                    String.format(
                        "Hash check is enabled but hashToken wasn't provided for the document: %s",
                        documentHashToken.getId()
                    ));
            } else {
                // document metadata exists and document is not a moving case
                if (documentMetadata.isPresent()
                    && !documentMetadata.get().getMetadata().isEmpty()
                    && !isDocumentMovingCases(documentMetadata.get().getCaseTypeId())) {
                    throw new BadRequestException(String.format(
                        "Document metadata exists for %s but the case type is not a moving case type: %s",
                        documentHashToken.getId(), documentMetadata.get().getCaseTypeId()
                    ));
                }
            }

            final DocumentUpdate documentUpdate = new DocumentUpdate(
                documentHashToken.getId(),
                Map.of(
                    METADATA_CASE_ID, caseDocumentsMetadata.getCaseId(),
                    METADATA_CASE_TYPE_ID, caseDocumentsMetadata.getCaseTypeId(),
                    METADATA_JURISDICTION_ID, caseDocumentsMetadata.getJurisdictionId()
                )
            );

            documentsList.add(documentUpdate);
        }

        return new UpdateDocumentsCommand(NULL_TTL, documentsList);
    }

    private void verifyHashTokenValidity(DocumentHashToken documentHashToken,
                                         Document documentMetadata) {
        String hashcodeFromStoredDocument =
            generateHashToken(documentHashToken.getId(), documentMetadata);
        if (!hashcodeFromStoredDocument.equals(documentHashToken.getHashToken())) {
            throw new ForbiddenException(String.format("Hash token check failed for the document: %s",
                                                       documentHashToken.getId()));
        }
    }

    private boolean isDocumentMovingCases(String documentCaseTypeId) {
        return applicationParams.getBulkScanExceptionRecordTypes().contains(documentCaseTypeId);
    }

    @Override
    public String generateHashToken(UUID documentId, Document document) {
        final String salt = applicationParams.getSalt();

        return (document.getCaseId() == null)
                ? ApplicationUtils.generateHashCode(salt.concat(documentId.toString()
                                                                    .concat(document.getJurisdictionId())
                                                                    .concat(document.getCaseTypeId())))
                : ApplicationUtils.generateHashCode(salt.concat(documentId.toString()
                                                                    .concat(document.getCaseId())
                                                                    .concat(document.getJurisdictionId())
                                                                    .concat(document.getCaseTypeId())));
    }

    @Override
    public String generateHashToken(UUID documentId) {
        return documentStoreClient.getDocument(documentId)
            .map(document -> generateHashToken(documentId, document))
            .orElse("");
    }

    @Override
    public UploadResponse uploadDocuments(final DocumentUploadRequest documentUploadRequest) {
        final DmUploadResponse dmResponse = documentStoreClient.uploadDocuments(documentUploadRequest);

        return buildUploadResponse(documentUploadRequest.getCaseTypeId(),
                                   documentUploadRequest.getJurisdictionId(),
                                   dmResponse);
    }

    @Override
    public PatchDocumentResponse patchDocument(final UUID documentId, final UpdateTtlRequest ttl) {
        final DmTtlRequest dmTtlRequest = new DmTtlRequest(ttl.getTtl().atZone(ZoneId.systemDefault()));
        return documentStoreClient.patchDocument(documentId, dmTtlRequest)
            .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NOT_FOUND));
    }

    @Override
    public void deleteDocument(final UUID documentId, final Boolean permanent) {
        documentStoreClient.deleteDocument(documentId, permanent);
    }

    private UploadResponse buildUploadResponse(final String caseTypeId,
                                               final String jurisdictionId,
                                               final DmUploadResponse dmResponse) {
        final List<Document> dmDocuments = Optional.ofNullable(dmResponse)
            .map(x -> x.getEmbedded().getDocuments())
            .orElse(emptyList());

        final List<Document> documents = dmDocuments.stream()
            .map(doc -> documentWithHashToken(doc, caseTypeId, jurisdictionId))
            .collect(Collectors.toList());

        return new UploadResponse(documents);
    }

    private Document documentWithHashToken(final Document dmDocument,
                                           final String caseTypeId,
                                           final String jurisdictionId) {
        final String href = dmDocument.getLinks().self.href;
        final String hashToken = ApplicationUtils.generateHashCode(applicationParams.getSalt().concat(
            href.substring(href.length() - 36)
                .concat(jurisdictionId)
                .concat(caseTypeId)));

        return dmDocument.toBuilder().hashToken(hashToken).build();
    }

    @Override
    public void checkUserPermission(final String caseId,
                                    final UUID documentId,
                                    final Permission permissionToCheck,
                                    final String logMessage,
                                    final String exceptionMessage) {

        final DocumentPermissions documentPermissions = caseDataStoreClient
            .getCaseDocumentMetadata(caseId, documentId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));

        if (!documentPermissions.getId().equals(documentId)
            || !documentPermissions.getPermissions().contains(permissionToCheck)) {
            log.error(logMessage, HttpStatus.FORBIDDEN);
            throw new ForbiddenException(exceptionMessage);
        }
    }

    @Override
    public void checkServicePermission(final String caseTypeId,
                                       final String jurisdictionId,
                                       final String serviceId,
                                       final Permission permission,
                                       final String logMessage,
                                       final String exceptionMessage) {
        AuthorisedService serviceConfig = getServiceDetailsFromJson(serviceId);
        if (!validateCaseTypeId(serviceConfig, caseTypeId)
            || !validateJurisdictionId(serviceConfig, jurisdictionId)
            || !validatePermissions(serviceConfig, permission)
        ) {
            log.error(logMessage, HttpStatus.FORBIDDEN);
            throw new ForbiddenException(exceptionMessage);
        }
    }

    private boolean validateCaseTypeId(AuthorisedService serviceConfig, String caseTypeId) {
        List<String> caseTypeIds = serviceConfig.getCaseTypeId();
        boolean result =
            !StringUtils.isEmpty(caseTypeId) && (caseTypeIds.contains("*") || caseTypeIds.contains(caseTypeId));

        log.info("Case Type Id is {} and validation result is {}", sanitiseData(caseTypeId), result);

        return result;
    }

    private boolean validateJurisdictionId(AuthorisedService serviceConfig, String jurisdictionId) {
        boolean result =
            !StringUtils.isEmpty(jurisdictionId) && (serviceConfig.getJurisdictionId().equals("*")
                || serviceConfig.getJurisdictionId().equals(jurisdictionId));

        log.info("JurisdictionI Id is {} and validation result is {}", sanitiseData(jurisdictionId), result);

        return result;
    }

    private String sanitiseData(String value) {
        return value.replaceAll("[\n|\r|\t]", "_");
    }

    private boolean validatePermissions(AuthorisedService serviceConfig, Permission permission) {
        List<Permission> servicePermissions = serviceConfig.getPermissions();
        boolean result = !servicePermissions.isEmpty() && (servicePermissions.contains(permission));
        log.info("Permission is {} and validation result is {}", permission, result);
        return result;
    }

    private AuthorisedService getServiceDetailsFromJson(String serviceId) {
        return authorisedServices.getAuthServices().stream()
            .filter(service -> service.getId().equals(serviceId))
            .findAny()
            .orElseThrow(() -> {
                log.error("Service Id {} is not authorized to access API ", serviceId);
                throw new ForbiddenException(String.format(Constants.EXCEPTION_SERVICE_ID_NOT_AUTHORISED, serviceId));
            });
    }

}
