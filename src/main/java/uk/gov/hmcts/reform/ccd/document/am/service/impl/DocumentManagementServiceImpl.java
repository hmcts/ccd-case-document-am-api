package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import feign.FeignException;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.ErrorResponse;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.document.am.controller.feign.DocumentStoreFeignClient;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadDocumentsCommand;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.util.JsonFeignResponseHelper;

import java.util.UUID;

@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private DocumentStoreFeignClient documentStoreFeignClient;

    @Autowired
    public DocumentManagementServiceImpl(DocumentStoreFeignClient documentStoreFeignClient) {
        this.documentStoreFeignClient = documentStoreFeignClient;
    }

    @Override
    public ResponseEntity getDocumentMetadata(UUID documentId) {

        try (Response response= documentStoreFeignClient.getMetadataForDocument(documentId);) {
            Class clazz = response.status() != 200 ? ErrorResponse.class : StoredDocumentHalResource.class;
            return JsonFeignResponseHelper.toResponseEntity(response, clazz);
        }  catch (FeignException ex) {
            log.error("Document Store api failed:: status code ::" + ex.status());
            throw new InvalidRequest("Document Store api failed!!");
        }

    }

    @Override
    public String extractDocumentMetadata(StoredDocumentHalResource storedDocument) {
        return null;
    }

    @Override
    public ResponseEntity<?> getDocumentBinaryContent(UUID documentId) {
        return null;
    }

    @Override
    public StoredDocumentHalResourceCollection uploadDocumentsContent(UploadDocumentsCommand uploadDocumentsContent) {
        return null;
    }
}
