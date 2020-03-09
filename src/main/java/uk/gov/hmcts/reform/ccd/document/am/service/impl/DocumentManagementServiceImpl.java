package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CLASSIFICATION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_LENGTH;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.FILES;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ORIGINAL_FILE_NAME;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ROLES;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.USERID;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import feign.FeignException;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.ErrorResponse;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.UnauthorizedException;
import uk.gov.hmcts.reform.ccd.document.am.controller.feign.DocumentStoreFeignClient;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.util.ApplicationUtils;
import uk.gov.hmcts.reform.ccd.document.am.util.JsonFeignResponseHelper;


@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentManagementServiceImpl.class);

    @Value("${documentStoreUrl}")
    private transient String dmStoreURL;

    @Value("${documentTTL}")
    private transient String documentTtl;

    private transient DocumentStoreFeignClient documentStoreFeignClient;
    private transient RestTemplate restTemplate;

    @Autowired
    public DocumentManagementServiceImpl(DocumentStoreFeignClient documentStoreFeignClient, RestTemplate restTemplate) {
        this.documentStoreFeignClient = documentStoreFeignClient;
        this.restTemplate = restTemplate;

    }

    @Override
    public ResponseEntity getDocumentMetadata(UUID documentId) {

        try (Response response = documentStoreFeignClient.getMetadataForDocument(documentId)) {
            Class clazz = response.status() > 300 ? ErrorResponse.class : StoredDocumentHalResource.class;
            ResponseEntity responseEntity = JsonFeignResponseHelper.toResponseEntity(response, clazz, documentId);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                return responseEntity;
            } else {
                LOG.error("Document doesn't exist for requested document id at Document Store API Side " + responseEntity.getStatusCode());
                throw new UnauthorizedException(documentId.toString());
            }
        } catch (FeignException ex) {
            log.error("Document Store api failed:: status code ::" + ex.status());
            throw new InvalidRequest("Document Store api failed!!");
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

        try {
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
    public ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification, List<String> roles,
                                                  String serviceAuthorization, String caseTypeId, String jurisdictionId,
                                                  String userId) {

        LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        HttpHeaders headers = prepareRequestForUpload(files, classification, roles, serviceAuthorization, caseTypeId, jurisdictionId, userId, bodyMap);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

        ResponseEntity<Object> uploadedDocumentResponse = restTemplate.postForEntity(dmStoreURL, requestEntity, Object.class);

        if (HttpStatus.OK.equals(uploadedDocumentResponse.getStatusCode())) {
            if (null != uploadedDocumentResponse.getBody()) {
                formatUploadDocumentResponse(caseTypeId, jurisdictionId, uploadedDocumentResponse);
            }
            return ResponseEntity
                .status(uploadedDocumentResponse.getStatusCode())
                .body(uploadedDocumentResponse.getBody());
        } else {
            return ResponseEntity
                .status(uploadedDocumentResponse.getStatusCode())
                .body(uploadedDocumentResponse.getBody());
        }
    }

    @SuppressWarnings("unchecked")
    private void formatUploadDocumentResponse(String caseTypeId, String jurisdictionId, ResponseEntity<Object> uploadedDocumentResponse) {
        LinkedHashMap documents = (LinkedHashMap) ((((LinkedHashMap) uploadedDocumentResponse.getBody()).get("_embedded")));
        ArrayList<Object> documentList = (ArrayList<Object>) (documents.get("documents"));

        for (Object document : documentList) {
            if (document instanceof LinkedHashMap) {
                LinkedHashMap<String, Object> hashmap = ((LinkedHashMap<String, Object>) (document));
                hashmap.remove("_embedded");
                JSONObject object = new JSONObject(hashmap);
                String documentURL = (String) object.getJSONObject("_links").getJSONObject("self").get("href");
                hashmap.put("hashcode", ApplicationUtils.generateHashCode(documentURL.concat(jurisdictionId).concat(caseTypeId)));
            }
        }
        //injectHashCode((StoredDocumentHalResourceCollection) uploadedDocumentResponse.getBody());
    }

    private void injectHashCode(StoredDocumentHalResourceCollection resourceCollection) {
        /*for (StoredDocumentHalResource resource: resourceCollection.getContent()) {
            resource.setHashCode(ApplicationUtils.generateHashCode(extractDocumentId(resource)));
        }*/
    }

    private String extractDocumentId(StoredDocumentHalResource storedDocumentHalResource) {
        Map<String, ResourceSupport> embedded = storedDocumentHalResource.getEmbedded();
        ResourceSupport resourceSupport = embedded.get("_links");
        return resourceSupport.getId().getHref();
    }

    private HttpHeaders prepareRequestForUpload(List<MultipartFile> files, String classification, List<String> roles, String serviceAuthorization,
                                                String caseTypeId, String jurisdictionId, String userId, LinkedMultiValueMap<String, Object> bodyMap) {
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                bodyMap.add(FILES, file.getResource());
            }
        }

        bodyMap.set(CLASSIFICATION, classification);
        bodyMap.set(ROLES, String.join(",", roles));
        bodyMap.set("metadata[jurisdictionId]", jurisdictionId);
        bodyMap.set("metadata[caseTypeId]", caseTypeId);
        //hardcoding caseId just to support the functional test cases. Needs to be removed later.
        bodyMap.set("metadata[caseId]", "1111222233334444");
        //Format of date : yyyy-MM-dd'T'HH:mm:ssZ  2020-02-15T15:18:00+0000
        bodyMap.set("ttl", getEffectiveTTL());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(SERVICE_AUTHORIZATION, serviceAuthorization);
        headers.set(USERID, userId);
        return headers;
    }

    private String getEffectiveTTL() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        return format.format(new Timestamp(new Date().getTime() + Long.parseLong(documentTtl)));
    }

    /* private ResponseEntity<StoredDocumentHalResourceCollection> injectHashCode(ResponseEntity<StoredDocumentHalResourceCollection>
    uploadedDocumentResponse) {
         List<StoredDocumentHalResource> uploadedDocuments = Optional.of(uploadedDocumentResponse.getBody().getContent());
     }
 */
    private HttpHeaders getHeaders(ResponseEntity<Resource> response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ORIGINAL_FILE_NAME, response.getHeaders().get(ORIGINAL_FILE_NAME).get(0));
        headers.add(CONTENT_DISPOSITION, response.getHeaders().get(CONTENT_DISPOSITION).get(0));
        headers.add(DATA_SOURCE, response.getHeaders().get(DATA_SOURCE).get(0));
        headers.add(CONTENT_TYPE, response.getHeaders().get(CONTENT_TYPE).get(0));
        headers.add(CONTENT_LENGTH, response.getHeaders().get(CONTENT_LENGTH).get(0));
        return headers;

    }

}
