package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import java.util.Map;
import java.util.UUID;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.feign.DocumentStoreFeignClient;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadDocumentsCommand;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;


@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private transient DocumentStoreFeignClient documentStoreFeignClient;
    private static final int RESOURCE_NOT_FOUND = 404;

    @Value("${documentStoreUrl}")
    private transient String dmStoreURL;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    public DocumentManagementServiceImpl(DocumentStoreFeignClient documentStoreFeignClient) {
        this.documentStoreFeignClient = documentStoreFeignClient;

    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity getDocumentMetadata(UUID documentId) {
        //logging with Log.error for development purpose.
        log.error("Getting Metadata for " + documentId);
        try {
            String documentURL = dmStoreURL + "/" + documentId;
            log.error("Getting Metadata using the URL : " + documentURL);
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            ResponseEntity<StoredDocumentHalResource> responseEntity = restTemplate.exchange(documentURL, HttpMethod.GET,
                                                                  requestEntity, StoredDocumentHalResource.class);

            responseEntity.getBody().addLinks(documentId);
            log.error("Response from Document Store Client: " + responseEntity.getStatusCode());
            return responseEntity;
        } catch (Exception ex) {
            if (ex instanceof HttpClientErrorException
                && ((HttpClientErrorException) ex).getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException("Resource not found ");
            } else {
                throw new ServiceException("Document Store error message::", ex);
            }
        }
    }

    @Override
    public String extractCaseIdFromMetadata(Object storedDocument) {
        if (storedDocument instanceof StoredDocumentHalResource) {
            Map<String, String> metadata = ((StoredDocumentHalResource) storedDocument).getMetadata();
            return metadata.get("caseId");
        }
        return null;
    }

    @Override
    public ResponseEntity<Resource> getDocumentBinaryContent(UUID documentId) {

        try {
            return documentStoreFeignClient.getDocumentBinary(documentId);

        } catch (FeignException ex) {
            log.error("Requested document could not be downloaded, DM Store Response Code ::" + ex.getMessage());
            throw new ResourceNotFoundException("Cannot download document that is stored");
        }
    }

    @Override
    public StoredDocumentHalResourceCollection uploadDocumentsContent(UploadDocumentsCommand uploadDocumentsContent) {
        return null;
    }


}
