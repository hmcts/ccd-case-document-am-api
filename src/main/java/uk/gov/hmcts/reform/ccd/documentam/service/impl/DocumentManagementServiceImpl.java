package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentUpdate;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.documentam.util.ApplicationUtils;
import uk.gov.hmcts.reform.ccd.documentam.util.ResponseHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DM_DATE_TIME_FORMATTER;

@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private static final Date NULL_TTL = null;

    private final RestTemplate restTemplate;

    @Value("${documentStoreUrl}")
    protected String documentURL;

    @Value("${documentTtlInDays}")
    protected int documentTtlInDays;

    @Value("${idam.s2s-auth.totp_secret}")
    protected String salt;

    private final CaseDataStoreService caseDataStoreService;

    @Value("${hash.check.enabled}")
    private boolean hashCheckEnabled;

    @Value("${bulkscan.exception.record.types}")
    private List<String> bulkScanExceptionRecordTypes;

    private final AuthorisedServices authorisedServices;

    private static final HttpEntity<Object> NULL_REQUEST_ENTITY = null;

    @Autowired
    public DocumentManagementServiceImpl(RestTemplate restTemplate, CaseDataStoreService caseDataStoreService,
                                         AuthorisedServices authorisedServices) {
        this.restTemplate = restTemplate;
        this.caseDataStoreService = caseDataStoreService;
        this.authorisedServices = authorisedServices;
    }

    @Override
    public Optional<StoredDocumentHalResource> getDocumentMetadata(UUID documentId) {
        ResponseEntity<StoredDocumentHalResource> responseResult = new ResponseEntity<>(HttpStatus.OK);

        try {
            String documentMetadataUrl = String.format("%s/documents/%s", documentURL, documentId);
            ResponseEntity<StoredDocumentHalResource> response = restTemplate.exchange(
                documentMetadataUrl,
                GET,
                NULL_REQUEST_ENTITY,
                StoredDocumentHalResource.class
            );
            ResponseEntity<StoredDocumentHalResource> responseEntity =
                ResponseHelper.toResponseEntity(response, documentId);

            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                responseResult = responseEntity;
            } else {
                log.error("Document doesn't exist for requested document id at Document Store {}", responseEntity
                    .getStatusCode());
                throw new ResourceNotFoundException(formatNotFoundMessage(documentId.toString()));
            }
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                return Optional.empty();
            }
            handleException(exception, documentId.toString());
        }
        return Optional.of(responseResult.getBody());
    }

    @Override
    public String extractCaseIdFromMetadata(StoredDocumentHalResource storedDocument) {
        if (storedDocument instanceof StoredDocumentHalResource) {
            Map<String, String> metadata = storedDocument.getMetadata();
            return metadata.get("caseId");
        }
        return null;
    }

    @Override
    public ResponseEntity<ByteArrayResource> getDocumentBinaryContent(UUID documentId) {
        ResponseEntity<ByteArrayResource> responseResult = new ResponseEntity<>(HttpStatus.OK);
        try {
            String documentBinaryUrl = String.format("%s/documents/%s/binary", documentURL, documentId);
            ResponseEntity<ByteArrayResource> response = restTemplate.exchange(
                documentBinaryUrl,
                GET,
                NULL_REQUEST_ENTITY,
                ByteArrayResource.class
            );
            if (HttpStatus.OK.equals(response.getStatusCode())) {
                responseResult = ResponseEntity.ok().headers(getHeaders(response))
                    .body(response.getBody());
            } else {
                responseResult = ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());
            }

        } catch (HttpClientErrorException exception) {
            handleException(exception, documentId.toString());
        }
        return responseResult;

    }

    @Override
    public void patchDocumentMetadata(CaseDocumentsMetadata caseDocumentsMetadata) {
        try {
            UpdateDocumentsCommand updateDocumentsCommand
                = prepareRequestForAttachingDocumentToCase(caseDocumentsMetadata);
            HttpEntity<UpdateDocumentsCommand> requestEntity = new HttpEntity<>(updateDocumentsCommand);
            String documentUrl = String.format("%s/documents", documentURL);
            restTemplate.exchange(documentUrl, HttpMethod.PATCH, requestEntity, Void.class);
        } catch (HttpClientErrorException exception) {
            handleException(exception);
        }
    }

    private UpdateDocumentsCommand prepareRequestForAttachingDocumentToCase(CaseDocumentsMetadata
                                                                                caseDocumentsMetadata) {
        List<DocumentUpdate> documentsList = new ArrayList<>();
        for (DocumentHashToken documentHashToken : caseDocumentsMetadata.getDocumentHashTokens()) {

            Optional<StoredDocumentHalResource> documentMetadata =
                getDocumentMetadata(documentHashToken.getId());


            if (documentHashToken.getHashToken() != null) {
                if (documentMetadata.isEmpty()) {
                    throw new ResourceNotFoundException(String.format("Meta data does not exist for documentId: %s",
                                                                      documentHashToken.getId()));
                }
                verifyHashTokenValidity(documentHashToken, documentMetadata.get());
            } else if (hashCheckEnabled) {
                throw new ForbiddenException(
                    String.format(
                        "Hash check is enabled but hashToken wasn't provided for the document:%s",
                        documentHashToken.getId()
                    ));
            } else {
                // document metadata exists and document is not a moving case
                if (documentMetadata.isPresent()
                    && !documentMetadata.get().getMetadata().isEmpty()
                    && !isDocumentMovingCases(documentMetadata.get().getMetadata().get(CASE_TYPE_ID))) {
                    throw new BadRequestException(String.format(
                        "Document metadata exists for %s but the case type is not a moving case type: %s",
                        documentHashToken.getId(), documentMetadata.get().getMetadata().get(CASE_TYPE_ID)
                    ));
                }
            }


            Map<String, String> metadataMap = new HashMap<>();
            metadataMap.put(Constants.CASE_ID, caseDocumentsMetadata.getCaseId());

            DocumentUpdate documentUpdate = new DocumentUpdate();
            documentUpdate.setDocumentId(documentHashToken.getId());
            documentUpdate.setMetadata(metadataMap);

            documentsList.add(documentUpdate);
        }

        return new UpdateDocumentsCommand(NULL_TTL, documentsList);
    }

    private void verifyHashTokenValidity(DocumentHashToken documentHashToken,
                                         StoredDocumentHalResource documentMetadata) {
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
    public String generateHashToken(UUID documentId, StoredDocumentHalResource documentMetaData) {
        String hashcodeFromStoredDocument = "";

        if (documentMetaData.getMetadata().get(Constants.CASE_ID) == null) {
            hashcodeFromStoredDocument = ApplicationUtils
                .generateHashCode(salt.concat(documentId.toString()
                    .concat(documentMetaData.getMetadata().get(Constants.JURISDICTION_ID))
                    .concat(documentMetaData.getMetadata().get(Constants.CASE_TYPE_ID))));
        } else {
            hashcodeFromStoredDocument = ApplicationUtils
                .generateHashCode(salt.concat(documentId.toString()
                    .concat(documentMetaData.getMetadata().get(Constants.CASE_ID))
                    .concat(documentMetaData.getMetadata().get(Constants.JURISDICTION_ID))
                    .concat(documentMetaData.getMetadata().get(Constants.CASE_TYPE_ID))));
        }

        return hashcodeFromStoredDocument;
    }

    @Override
    public UploadResponse uploadDocuments(List<MultipartFile> files, String classification,
                                          String caseTypeId, String jurisdictionId) {
        DmUploadResponse dmResponse = null;
        try {
            LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
            HttpHeaders headers = prepareRequestForUpload(classification, caseTypeId, jurisdictionId, bodyMap);

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    bodyMap.add(Constants.FILES, file.getResource());
                }
            }
            String docUrl = String.format("%s/documents", documentURL);

            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
            dmResponse = restTemplate.postForObject(docUrl, requestEntity, DmUploadResponse.class);
        } catch (HttpClientErrorException exception) {
            handleException(exception);
        }
        return buildUploadResponse(caseTypeId, jurisdictionId, dmResponse);
    }

    @Override
    public ResponseEntity<PatchDocumentResponse> patchDocument(UUID documentId, UpdateTtlRequest ttl) {
        ResponseEntity<PatchDocumentResponse> responseResult = new ResponseEntity<>(HttpStatus.OK);

        try {
            DmTtlRequest dmTtlRequest = new DmTtlRequest(ttl.getTtl().atZone(ZoneId.systemDefault()));
            final HttpEntity<DmTtlRequest> requestEntity = new HttpEntity<>(dmTtlRequest);
            String patchTTLUrl = String.format("%s/documents/%s", documentURL, documentId);
            ResponseEntity<StoredDocumentHalResource> response = restTemplate.exchange(
                patchTTLUrl,
                PATCH,
                requestEntity,
                StoredDocumentHalResource.class
            );
            ResponseEntity<PatchDocumentResponse> responseEntity = ResponseHelper.updatePatchTTLResponse(response);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                responseResult = responseEntity;
            } else {
                log.error("Document doesn't exist for requested document id at Document Store API Side {}", response
                    .getStatusCode());
                throw new ResourceNotFoundException(formatNotFoundMessage(documentId.toString()));
            }
        } catch (HttpClientErrorException exception) {
            handleException(exception, documentId.toString());
        }
        return responseResult;
    }

    @Override
    public void deleteDocument(UUID documentId, Boolean permanent) {
        final String documentDeleteUrl = String.format(
            "%s/documents/%s?permanent=%s",
            documentURL,
            documentId,
            permanent
        );
        try {
            restTemplate.exchange(documentDeleteUrl, DELETE, NULL_REQUEST_ENTITY, Void.class);
        } catch (HttpClientErrorException exception) {
            handleException(exception, documentId.toString());
        }
    }

    private UploadResponse buildUploadResponse(String caseTypeId, String jurisdictionId, DmUploadResponse dmResponse) {
        List<Document> dmDocuments = dmResponse.getEmbedded().getDocuments();

        List<Document> documents = dmDocuments.stream()
            .map(doc -> documentWithHashToken(doc, caseTypeId, jurisdictionId))
            .collect(Collectors.toList());
        return new UploadResponse(documents);
    }

    private Document documentWithHashToken(Document dmDocument, String caseTypeId, String jurisdictionId) {
        String href = dmDocument.getLinks().self.href;
        String hashToken = ApplicationUtils.generateHashCode(salt.concat(
            href.substring(href.length() - 36)
                .concat(jurisdictionId)
                .concat(caseTypeId)));
        return dmDocument.toBuilder().hashToken(hashToken).build();
    }

    private HttpHeaders prepareRequestForUpload(String classification,
                                                String caseTypeId, String jurisdictionId,
                                                LinkedMultiValueMap<String, Object> bodyMap) {
        bodyMap.set(Constants.CLASSIFICATION, classification);
        bodyMap.set("metadata[jurisdictionId]", jurisdictionId);
        bodyMap.set("metadata[caseTypeId]", caseTypeId);
        bodyMap.set("ttl", getEffectiveTTL());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    private String getEffectiveTTL() {
        ZonedDateTime currentDateTime = ZonedDateTime.now();
        return currentDateTime.plusDays(documentTtlInDays).format(DM_DATE_TIME_FORMATTER);
    }

    private HttpHeaders getHeaders(ResponseEntity<ByteArrayResource> response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(Constants.ORIGINAL_FILE_NAME, response.getHeaders().get(Constants.ORIGINAL_FILE_NAME).get(0));
        headers.add(Constants.CONTENT_DISPOSITION, response.getHeaders().get(Constants.CONTENT_DISPOSITION).get(0));
        headers.add(Constants.DATA_SOURCE, response.getHeaders().get(Constants.DATA_SOURCE).get(0));
        headers.add(Constants.CONTENT_TYPE, response.getHeaders().get(Constants.CONTENT_TYPE).get(0));
        return headers;

    }

    @Override
    public void checkUserPermission(StoredDocumentHalResource documentMetadata,
                                       UUID documentId, Permission permissionToCheck,
                                       String logMessage, String exceptionMessage) {
        final String caseId = extractCaseIdFromMetadata(documentMetadata);

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
    public void checkServicePermission(StoredDocumentHalResource documentMetadata,
                                       String serviceId, Permission permission,
                                       String logMessage, String exceptionMessage) {
        AuthorisedService serviceConfig = getServiceDetailsFromJson(serviceId);
        String caseTypeId = extractCaseTypeIdFromMetadata(documentMetadata);
        String jurisdictionId = extractJurisdictionIdFromMetadata(documentMetadata);
        if (!validateCaseTypeId(serviceConfig, caseTypeId)
            || !validateJurisdictionId(serviceConfig, jurisdictionId)
            || !validatePermissions(serviceConfig, permission)
        ) {
            log.error(logMessage, HttpStatus.FORBIDDEN);
            throw new ForbiddenException(exceptionMessage);
        }
    }

    @Override
    public void checkServicePermission(String caseTypeId, String jurisdictionId,
                                       String serviceId, Permission permission,
                                       String logMessage, String exceptionMessage) {
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

    private String extractCaseTypeIdFromMetadata(Object storedDocument) {
        if (storedDocument instanceof StoredDocumentHalResource) {
            Map<String, String> metadata = ((StoredDocumentHalResource) storedDocument).getMetadata();
            return metadata.get(Constants.CASE_TYPE_ID);
        }
        return null;
    }

    private String extractJurisdictionIdFromMetadata(Object storedDocument) {
        if (storedDocument instanceof StoredDocumentHalResource) {
            Map<String, String> metadata = ((StoredDocumentHalResource) storedDocument).getMetadata();
            return metadata.get(Constants.JURISDICTION_ID);
        }
        return null;
    }

    private void handleException(HttpClientErrorException exception, String messageParam) {
        if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
            throw new ResourceNotFoundException(formatNotFoundMessage(messageParam), exception);
        } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
            throw new ForbiddenException(messageParam, exception);
        } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
            throw new BadRequestException(messageParam, exception);
        } else {
            throw new ServiceException(String.format(Constants.EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE, messageParam),
                exception);
        }
    }

    private void handleException(HttpClientErrorException exception) {
        if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
            throw new ResourceNotFoundException(Constants.RESOURCE_NOT_FOUND, exception);
        } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
            throw new ForbiddenException(Constants.FORBIDDEN, exception);
        } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
            throw new BadRequestException(Constants.BAD_REQUEST, exception);
        } else {
            throw new ServiceException(Constants.EXCEPTION_ERROR_MESSAGE, exception);
        }
    }

    private String formatNotFoundMessage(String resourceId) {
        return Constants.RESOURCE_NOT_FOUND + " " + resourceId;
    }
}
