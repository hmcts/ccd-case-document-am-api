package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import java.util.Map;
import java.util.UUID;

import feign.FeignException;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.ErrorResponse;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.feign.DocumentStoreFeignClient;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadDocumentsCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentStreamOject;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.util.JsonFeignResponseHelper;

@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private transient DocumentStoreFeignClient documentStoreFeignClient;


    @Autowired
    public DocumentManagementServiceImpl(DocumentStoreFeignClient documentStoreFeignClient) {
        this.documentStoreFeignClient = documentStoreFeignClient;

    }

    @Override
    public ResponseEntity getDocumentMetadata(UUID documentId) {

        try (Response response = documentStoreFeignClient.getMetadataForDocument(documentId)) {
            Class clazz = response.status() > 300 ? ErrorResponse.class : StoredDocumentHalResource.class;
            return JsonFeignResponseHelper.toResponseEntity(response, clazz, documentId);
        } catch (FeignException ex) {
            log.error("Document Store api failed:: status code ::" + ex.status());
            throw new InvalidRequest("Document Store api failed!!");
        }
    }

    @Override
    public String extractDocumentMetadata(Object storedDocument) {
        if (storedDocument instanceof StoredDocumentHalResource) {
            Map<String,String> metadata = ((StoredDocumentHalResource) storedDocument).getMetadata();
            return metadata.get("caseId");
        }
        return null;
    }

    @Override
    public ResponseEntity getDocumentBinaryContent(UUID documentId) {

        try  {
            ResponseEntity<Resource> response = documentStoreFeignClient.getDocumentBinary(documentId);

            if (HttpStatus.OK.equals(response.getStatusCode())) {
                return ResponseEntity.status(response.getStatusCode()).body(new DocumentStreamOject(response.getBody(),
                                                                                                    response.getHeaders().get("originalfilename").get(0), response.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0)));
            } else {
                return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());
            }

        } catch (FeignException ex) {
            log.error("Cannot download document that is stored::" + ex.status());
            throw new ResourceNotFoundException("Cannot download document that is stored");
        }
    }

    @Override
    public StoredDocumentHalResourceCollection uploadDocumentsContent(UploadDocumentsCommand uploadDocumentsContent) {
        return null;
    }




}
