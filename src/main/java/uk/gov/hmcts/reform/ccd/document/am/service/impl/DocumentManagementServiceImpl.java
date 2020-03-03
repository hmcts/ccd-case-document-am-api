package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_LENGTH;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ORIGINAL_FILE_NAME;

import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.endpoints.CaseDocumentAmController;
import uk.gov.hmcts.reform.ccd.document.am.controller.feign.DocumentStoreFeignClient;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadDocumentsCommand;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;


@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentManagementServiceImpl.class);

    private transient DocumentStoreFeignClient documentStoreFeignClient;
    private static final int RESOURCE_NOT_FOUND = 404;

    @Value("${documentStoreUrl}")
    private transient String dmStoreURL;

    @Autowired
    private transient RestTemplate restTemplate;

    @Autowired
    private transient SecurityUtils securityUtils;

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
            ResponseEntity<StoredDocumentHalResource> responseEntity =
                restTemplate.exchange(documentURL, HttpMethod.GET, requestEntity, StoredDocumentHalResource.class);

            responseEntity.getBody().add(
                linkTo(methodOn(CaseDocumentAmController.class).getDocumentbyDocumentId("dsds", documentId, "323", "caseworker-1")).withSelfRel());
            responseEntity.getBody().add(
                linkTo(methodOn(CaseDocumentAmController.class).getDocumentBinaryContentbyDocumentId("dsds", documentId, "323", "caseworker-1"))
                    .withRel("binary"));

            log.error("Response from Document Store Client: " + responseEntity.getStatusCode());
            return responseEntity;
        } catch (HttpClientErrorException ex) {
            throw new ResourceNotFoundException("Resource not found ");
        } catch (Exception ex) {
            throw new ServiceException("Document Store error message::", ex);
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
    public ResponseEntity<Object> getDocumentBinaryContent(UUID documentId) {

        try  {
            ResponseEntity<Resource> response = null;
            /*documentStoreFeignClient.getDocumentBinary(documentId);*/

            if (HttpStatus.OK.equals(response.getStatusCode())) {
                return ResponseEntity.ok().headers(getHeaders(response))
                    .body((ByteArrayResource) response.getBody());
            } else {
                return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());
            }

        } catch (Exception ex) {
            log.error("Requested document could not be downloaded, DM Store Response Code ::" + ex.getMessage());
            throw new ResourceNotFoundException("Cannot download document that is stored");
        }
    }

    @Override
    public StoredDocumentHalResourceCollection uploadDocumentsContent(UploadDocumentsCommand uploadDocumentsContent) {
        return null;
    }

    private HttpHeaders getHeaders(ResponseEntity<Resource> response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ORIGINAL_FILE_NAME,response.getHeaders().get(ORIGINAL_FILE_NAME).get(0));
        headers.add(CONTENT_DISPOSITION,response.getHeaders().get(CONTENT_DISPOSITION).get(0));
        headers.add(DATA_SOURCE,response.getHeaders().get(DATA_SOURCE).get(0));
        headers.add(CONTENT_TYPE, response.getHeaders().get(CONTENT_TYPE).get(0));
        headers.add(CONTENT_LENGTH,response.getHeaders().get(CONTENT_LENGTH).get(0));
        return headers;

    }

}
