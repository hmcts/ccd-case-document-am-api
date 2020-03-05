package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import feign.FeignException;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.ErrorResponse;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadDocumentsCommand;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.util.JsonFeignResponseHelper;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpMethod.GET;
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

    @Value("${documentStoreUrl}") String documentURL;


    @Autowired
    public DocumentManagementServiceImpl(RestTemplate restTemplate, SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;

        this.securityUtils = securityUtils;
    }

    @Override
    public ResponseEntity getDocumentMetadata(UUID documentId) {

        try {
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            String documentMetadataUrl = String.format("%s/%s", documentURL, documentId);
            ResponseEntity<StoredDocumentHalResource> responseEntity = restTemplate.exchange(documentMetadataUrl, GET, requestEntity, StoredDocumentHalResource.class);
            Class clazz = responseEntity.getStatusCode().value() > 300 ? ErrorResponse.class : StoredDocumentHalResource.class;
            ResponseEntity responseEntity = JsonFeignResponseHelper.toResponseEntity(responseEntity, clazz, documentId);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                return responseEntity;
            } else {
                LOG.error("Document doesn't exist for requested document id at Document Store API Side " + responseEntity.getStatusCode());
                throw new ResourceNotFoundException(documentId.toString());
            }
        } catch (FeignException ex) {
            log.error("Document Store api failed:: status code ::" + ex.status());
            throw new InvalidRequest("Document Store api failed!!");
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
    public ResponseEntity<Object> getDocumentBinaryContent(UUID documentId) {

        try  {
            ResponseEntity<Resource> response = documentStoreFeignClient.getDocumentBinary(documentId);

            if (HttpStatus.OK.equals(response.getStatusCode())) {
                return ResponseEntity.ok().headers(getHeaders(response))
                    .body((ByteArrayResource) response.getBody());
            } else {
                return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());
            }

        } catch (FeignException ex) {
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
