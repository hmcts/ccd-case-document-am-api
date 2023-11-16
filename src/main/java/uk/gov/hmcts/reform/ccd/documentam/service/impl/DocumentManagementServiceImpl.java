package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.client.datastore.CaseDataStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DocumentStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UploadResponse;
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
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_JURISDICTION_ID;

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
            .fold(
                this::throwLeft,
                right -> right
            );
    }

    @Override
    public ResponseEntity<ByteArrayResource> getDocumentBinaryContent(UUID documentId) {
        return documentStoreClient.getDocumentAsBinary(documentId);
    }

    @Override
    public ResponseEntity<InputStreamResource> streamDocumentBinaryContent(UUID documentId) {
        return documentStoreClient.streamDocumentAsBinary(documentId);
    }

    @Override
    public void patchDocumentMetadata(final CaseDocumentsMetadata caseDocumentsMetadata) {
        final UpdateDocumentsCommand updateDocumentsCommand
            = prepareRequestForAttachingDocumentToCase(caseDocumentsMetadata);

        if (CollectionUtils.isNotEmpty(updateDocumentsCommand.getDocuments())) {
            documentStoreClient.patchDocumentMetadata(updateDocumentsCommand);
        }
    }

    @Override
    public void patchDocumentMetadata(UUID documentId, String caseTypeId, String jurisdictionId) {
        List<DocumentUpdate> documentsList = new ArrayList<>();
        final DocumentUpdate documentUpdate = new DocumentUpdate(
            documentId,
            Map.of(METADATA_CASE_TYPE_ID, caseTypeId,
                   METADATA_JURISDICTION_ID, jurisdictionId
            )
        );

        documentsList.add(documentUpdate);
        UpdateDocumentsCommand updateDocumentsCommand = new UpdateDocumentsCommand(NULL_TTL, documentsList);
        documentStoreClient.patchDocumentMetadata(updateDocumentsCommand);
    }

    private UpdateDocumentsCommand prepareRequestForAttachingDocumentToCase(CaseDocumentsMetadata
                                                                                caseDocumentsMetadata) {
        List<DocumentUpdate> documentsList = new ArrayList<>();
        for (DocumentHashToken documentHashToken : caseDocumentsMetadata.getDocumentHashTokens()) {
            final Either<ResourceNotFoundException, Document> either =
                documentStoreClient.getDocument(documentHashToken.getId());

            if (documentHashToken.getHashToken() != null) {
                if (either.isLeft()) {
                    throw either.getLeft();
                }
                if (shouldSkip(caseDocumentsMetadata.getCaseId(), documentHashToken.getId(), either.get())) {
                    continue;
                }
                verifyHashTokenValidity(documentHashToken, either.get());
            } else if (applicationParams.isHashCheckEnabled()) {
                throw new ForbiddenException(
                    String.format(
                        "Hash check is enabled but hashToken wasn't provided for the document: %s",
                        documentHashToken.getId()
                    ));
            } else {
                // document metadata exists and document is not a moving case
                if (either.isRight()
                    && !either.get().getMetadata().isEmpty()
                    && !allowMetadataOverride(either.get(), caseDocumentsMetadata, documentHashToken)) {
                    // graceful failure with warning.
                    log.warn("Document metadata already exists for docId:{} with caseType:{} and caseId:{}. "
                                 + "Cannot override with caseType:{}, caseId:{}",
                             documentHashToken.getId(), either.get().getCaseTypeId(), either.get().getCaseId(),
                             caseDocumentsMetadata.getCaseTypeId(), caseDocumentsMetadata.getCaseId());
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

    private boolean shouldSkip(final String caseId, final UUID documentId, final Document document) {
        if (document.getCaseId() != null) {
            if (caseId.equalsIgnoreCase(document.getCaseId())) {
                log.info("Document {} metadata is already attached to same caseId:{} - possibly due to concurrent"
                             + " ccd events", documentId, document.getCaseId());
                return true;
            }
        }
        return false;
    }

    private void verifyHashTokenValidity(DocumentHashToken documentHashToken,
                                         Document documentMetadata) {
        String hashcodeFromStoredDocument = generateHashToken(documentHashToken.getId(), documentMetadata.getCaseId(),
                              documentMetadata.getJurisdictionId(), documentMetadata.getCaseTypeId());
        if (!hashcodeFromStoredDocument.equals(documentHashToken.getHashToken())) {
            throw new ForbiddenException(String.format("Hash token check failed for the document: %s",
                                                       documentHashToken.getId()));
        }
    }

    private boolean allowMetadataOverride(Document document, CaseDocumentsMetadata caseDocumentsMetadata,
                                          DocumentHashToken documentHashToken) {
        boolean isMetadataExistsForSameCase = caseDocumentsMetadata.getCaseId().equalsIgnoreCase(document.getCaseId());
        if (isMetadataExistsForSameCase) {
            log.info("Document {} metadata already attached to same caseId:{} - possibly due to concurrent ccd events",
                     documentHashToken.getId(), document.getCaseId());
            return true;
        }

        boolean isOfMovingCaseType = applicationParams.getMovingCaseTypes().contains(document.getCaseTypeId());
        if (isOfMovingCaseType) {
            log.info("Document {} is trying to move From caseType:{}, caseId:{} To caseType:{}, caseId:{}",
                     documentHashToken.getId(), document.getCaseTypeId(), document.getCaseId(),
                     caseDocumentsMetadata.getCaseTypeId(), caseDocumentsMetadata.getCaseId());
        }
        return isOfMovingCaseType;
    }

    @Override
    public String generateHashToken(UUID documentId, String caseId, String jurisdictionId,
                                    String caseTypeId) {
        final String salt = applicationParams.getSalt();

        return (caseId == null)
            ? ApplicationUtils.generateHashCode(salt.concat(documentId.toString()
                                                                .concat(jurisdictionId)
                                                                .concat(caseTypeId)))
            : ApplicationUtils.generateHashCode(salt.concat(documentId.toString()
                                                                .concat(caseId)
                                                                .concat(jurisdictionId)
                                                                .concat(caseTypeId)));
    }

    @Override
    public String generateHashToken(UUID documentId, AuthorisedService authorisedService, Permission permission) {
        Either<ResourceNotFoundException, Document> either = documentStoreClient.getDocument(documentId);
        if (either.isLeft()) {
            return "";
        }
        Document documentMetaData = either.get();

        boolean metadataPatchRequired = false;

        String caseTypeId;
        if (StringUtils.isNotEmpty(documentMetaData.getCaseTypeId())) {
            caseTypeId = documentMetaData.getCaseTypeId();
        } else {
            if (authorisedService.getCaseTypeIdOptionalFor().contains(permission)) {
                caseTypeId = authorisedService.getDefaultCaseTypeForTokenGeneration();
                metadataPatchRequired = true;
            } else {
                throw new ForbiddenException("No case type id available to generate hash token for document "
                                                 + documentId);
            }
        }

        String jurisdictionId;
        if (StringUtils.isNotEmpty(documentMetaData.getJurisdictionId())) {
            jurisdictionId = documentMetaData.getJurisdictionId();
        } else {
            if (authorisedService.getJurisdictionIdOptionalFor().contains(permission)) {
                jurisdictionId = authorisedService.getDefaultJurisdictionForTokenGeneration();
                metadataPatchRequired = true;
            } else {
                throw new ForbiddenException("No jurisdiction id available to generate hash token for document "
                                                 + documentId);
            }
        }

        String hashToken = generateHashToken(documentId, documentMetaData.getCaseId(), jurisdictionId, caseTypeId);

        // if jurisdictionId or caseTypeId for a document is null, default values are retrieved from service_config
        // for the client, and if the client has sufficient permission to generate a hashToken
        // the document's metadata is patched with these values to allow management of the file to the client.
        if (metadataPatchRequired) {
            patchDocumentMetadata(documentId, caseTypeId, jurisdictionId);
        }

        return hashToken;
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
            .fold(
                this::throwLeft,
                right -> right
            );
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
    public AuthorisedService checkServicePermission(final String caseTypeId,
                                                    final String jurisdictionId,
                                                    final String serviceId,
                                                    final Permission permission,
                                                    final String logMessage,
                                                    final String exceptionMessage) {
        log.debug("checkServicePermission parameters: caseTypeId: {}, jurisdictionId: {}, serviceId: {}",
                  caseTypeId, jurisdictionId, serviceId);

        AuthorisedService serviceConfig = getServiceDetailsFromJson(serviceId);
        if (!validateCaseTypeId(serviceConfig, caseTypeId, permission)
            || !validateJurisdictionId(serviceConfig, jurisdictionId, permission)
            || !validatePermissions(serviceConfig, permission)
        ) {
            log.error(logMessage, HttpStatus.FORBIDDEN);
            throw new ForbiddenException(exceptionMessage);
        }

        return serviceConfig;
    }

    private <U> U throwLeft(final RuntimeException exception) {
        throw exception;
    }

    private boolean validateCaseTypeId(AuthorisedService serviceConfig, String caseTypeId, Permission permission) {
        List<String> caseTypeIds = serviceConfig.getCaseTypeId();
        boolean result =
            (StringUtils.isEmpty(caseTypeId) && serviceConfig.getCaseTypeIdOptionalFor().contains(permission))
                || (!StringUtils.isEmpty(caseTypeId)
                    && (caseTypeIds.contains("*") || caseTypeIds.contains(caseTypeId)));

        log.info("Case Type Id is {} and validation result is {}", caseTypeId, result);

        return result;
    }

    private boolean validateJurisdictionId(AuthorisedService serviceConfig, String jurisdictionId,
                                           Permission permission) {
        boolean result =
            (StringUtils.isEmpty(jurisdictionId) && serviceConfig.getJurisdictionIdOptionalFor().contains(permission))
            || (!StringUtils.isEmpty(jurisdictionId) && (serviceConfig.getJurisdictionId().equals("*")
                || serviceConfig.getJurisdictionId().equals(jurisdictionId)));

        log.info("JurisdictionI Id is {} and validation result is {}", jurisdictionId, result);

        return result;
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
