package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.ErrorResponse;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadDocumentsCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.service.common.ValidationService;
import uk.gov.hmcts.reform.ccd.document.am.util.ResponseHelper;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpMethod.GET;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_LENGTH;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ORIGINAL_FILE_NAME;


@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentManagementServiceImpl.class);

    private transient RestTemplate restTemplate;

    private transient SecurityUtils securityUtils;

    @Value("${documentStoreUrl}")
    private transient String documentURL;

    private transient CaseDataStoreService caseDataStoreService;
    private transient ValidationService validationService;

    @Autowired
    public DocumentManagementServiceImpl(RestTemplate restTemplate, SecurityUtils securityUtils,CaseDataStoreService caseDataStoreService,
                                         ValidationService validationService) {
        this.restTemplate = restTemplate;

        this.securityUtils = securityUtils;
        this.caseDataStoreService = caseDataStoreService;
        this.validationService = validationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity getDocumentMetadata(UUID documentId) {

        try {
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            String documentMetadataUrl = String.format("%s/%s", documentURL, documentId);
            ResponseEntity<StoredDocumentHalResource> response = restTemplate.exchange(
                documentMetadataUrl,
                GET,
                requestEntity,
                StoredDocumentHalResource.class
            );
            Class clazz = response.getStatusCode().value() > 300 ? ErrorResponse.class : StoredDocumentHalResource.class;
            ResponseEntity responseEntity = ResponseHelper.toResponseEntity(response, clazz, documentId);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                return responseEntity;
            } else {
                LOG.error("Document doesn't exist for requested document id at Document Store API Side " + responseEntity.getStatusCode());
                throw new ResourceNotFoundException(documentId.toString());
            }
        } catch (HttpClientErrorException ex) {
            log.error(ex.getMessage());
            if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
                throw new ResourceNotFoundException(documentId.toString());
            } else {
                throw new ServiceException(String.format(
                    "Problem  fetching the document for document id: %s because of %s",
                    documentId,
                    ex.getMessage()
                ));
            }

        }

    }

    @Override
    public String extractCaseIdFromMetadata(Object storedDocument) {
        if (storedDocument instanceof StoredDocumentHalResource) {
            Map<String,String> metadata = ((StoredDocumentHalResource) storedDocument).getMetadata();
            return metadata.get("caseId");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity<Object> getDocumentBinaryContent(UUID documentId) {
        try {
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            String documentBinaryUrl = String.format("%s/%s/binary", documentURL, documentId);
            ResponseEntity<ByteArrayResource> response = restTemplate.exchange(
                documentBinaryUrl,
                GET,
                requestEntity,
                ByteArrayResource.class
            );
            if (HttpStatus.OK.equals(response.getStatusCode())) {
                return ResponseEntity.ok().headers(getHeaders(response))
                    .body(response.getBody());
            } else {
                return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());
            }

        } catch (HttpClientErrorException ex) {
            log.error(ex.getMessage());
            if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
                throw new ResourceNotFoundException(documentId.toString());
            } else {
                throw new ServiceException(String.format(
                    "Problem  fetching the document binary for document id: %s because of %s",
                    documentId,
                    ex.getMessage()
                ));
            }

        }

    }

    @Override
    public StoredDocumentHalResourceCollection uploadDocumentsContent(UploadDocumentsCommand uploadDocumentsContent) {
        return null;
    }

    private HttpHeaders getHeaders(ResponseEntity<ByteArrayResource> response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ORIGINAL_FILE_NAME,response.getHeaders().get(ORIGINAL_FILE_NAME).get(0));
        headers.add(CONTENT_DISPOSITION,response.getHeaders().get(CONTENT_DISPOSITION).get(0));
        headers.add(DATA_SOURCE,response.getHeaders().get(DATA_SOURCE).get(0));
        headers.add(CONTENT_TYPE, response.getHeaders().get(CONTENT_TYPE).get(0));
        headers.add(CONTENT_LENGTH,response.getHeaders().get(CONTENT_LENGTH).get(0));
        return headers;

    }

    public boolean checkUserPermission(ResponseEntity responseEntity, UUID documentId) {
        String caseId = extractCaseIdFromMetadata(responseEntity.getBody());

        if (!validationService.validate(caseId)) {
            LOG.error(CASE_ID_INVALID + HttpStatus.BAD_REQUEST);
            throw new BadRequestException(CASE_ID_INVALID);

        } else {
            CaseDocumentMetadata caseDocumentMetadata = caseDataStoreService.getCaseDocumentMetadata(caseId, documentId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));
            if (caseDocumentMetadata.getDocument().get().getId().equals(documentId.toString())
                && caseDocumentMetadata.getDocument().get().getPermissions().contains(Permission.READ)) {
                return true;

            }

        }
        return false;
    }

}
