package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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
import uk.gov.hmcts.reform.ccd.documentam.service.CaseDataStoreService;
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
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DOCUMENT_METADATA_NOT_FOUND;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.RESOURCE_NOT_FOUND;

@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private static final Date NULL_TTL = null;

    @Value("${documentStoreUrl}")
    protected String documentURL;

    @Value("${documentTtlInDays}")
    protected int documentTtlInDays;

    @Value("${idam.s2s-auth.totp_secret}")
    protected String salt;

    @Value("${hash.check.enabled}")
    private boolean hashCheckEnabled;

    @Value("${bulkscan.exception.record.types}")
    private List<String> bulkScanExceptionRecordTypes;

    private final DocumentStoreClient documentStoreClient;

    private final CaseDataStoreService caseDataStoreService;

    private final AuthorisedServices authorisedServices;

    @Autowired
    public DocumentManagementServiceImpl(final DocumentStoreClient documentStoreClient,
                                         final CaseDataStoreService caseDataStoreService,
                                         final AuthorisedServices authorisedServices) {
        this.documentStoreClient = documentStoreClient;
        this.caseDataStoreService = caseDataStoreService;
        this.authorisedServices = authorisedServices;
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
            } else if (hashCheckEnabled) {
                throw new ForbiddenException(
                    String.format(
                        "Hash check is enabled but hashToken wasn't provided for the document: %s",
                        documentHashToken.getId()
                    ));
            } else {
                // document metadata exists and document is not a moving case
                if (documentMetadata.isPresent()
                    && !documentMetadata.get().getMetadata().isEmpty()
                    && !isDocumentMovingCases(documentMetadata.get().getMetadata().get(CASE_TYPE_ID))) {
                    throw new BadRequestException(String.format(
                        "Document metadata exists but the case type is not a moving case type: %s",
                        documentHashToken.getId()
                    ));
                }
            }

            final DocumentUpdate documentUpdate = new DocumentUpdate(
                documentHashToken.getId(),
                Map.of(CASE_ID, caseDocumentsMetadata.getCaseId())
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
        return bulkScanExceptionRecordTypes.contains(documentCaseTypeId);
    }

    @Override
    public String generateHashToken(UUID documentId, Document document) {
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
        final String hashToken = ApplicationUtils.generateHashCode(salt.concat(
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

        final DocumentPermissions documentPermissions = caseDataStoreService
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
        boolean result =
            !StringUtils.isEmpty(caseTypeId) && (serviceConfig.getCaseTypeId().equals("*") || caseTypeId.equals(
                serviceConfig.getCaseTypeId()));
        caseTypeId = sanitiseData(caseTypeId);
        log.info("Case Type Id is {} and validation result is {}", caseTypeId, result);
        return result;
    }

    private boolean validateJurisdictionId(AuthorisedService serviceConfig, String jurisdictionId) {
        boolean result =
            !StringUtils.isEmpty(jurisdictionId) && (serviceConfig.getJurisdictionId().equals("*")
                || jurisdictionId.equals(
                serviceConfig.getJurisdictionId()));
        jurisdictionId = sanitiseData(jurisdictionId);
        log.info("JurisdictionI Id is {} and validation result is {}", jurisdictionId, result);
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
        Optional<AuthorisedService> service =
            authorisedServices.getAuthServices().stream().filter(s -> s.getId().equals(
                serviceId)).findAny();
        if (service.isPresent()) {
            return service.get();
        } else {
            log.error("Service Id {} is not authorized to access API ", serviceId);
            throw new ForbiddenException(String.format(Constants.EXCEPTION_SERVICE_ID_NOT_AUTHORISED, serviceId));
        }
    }

}
