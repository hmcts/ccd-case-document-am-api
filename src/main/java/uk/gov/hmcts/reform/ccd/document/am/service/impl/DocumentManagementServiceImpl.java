package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.BINARY;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CLASSIFICATION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_LENGTH;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DOCUMENTS;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.EMBEDDED;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.FILES;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.HASHCODE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.HREF;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.LINKS;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ORIGINAL_FILE_NAME;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ROLES;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SELF;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.THUMBNAIL;
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
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.service.common.ValidationService;
import uk.gov.hmcts.reform.ccd.document.am.util.ApplicationUtils;
import uk.gov.hmcts.reform.ccd.document.am.util.ResponseHelper;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;


@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentManagementServiceImpl.class);

    private transient RestTemplate restTemplate;

    private transient SecurityUtils securityUtils;

    @Value("${documentStoreUrl}")
    protected transient String documentURL;

    @Value("${documentTTL}")
    protected transient String documentTtl;

    private transient CaseDataStoreService caseDataStoreService;

    @Autowired
    public DocumentManagementServiceImpl(RestTemplate restTemplate, SecurityUtils securityUtils,
                                         CaseDataStoreService caseDataStoreService) {
        this.restTemplate = restTemplate;

        this.securityUtils = securityUtils;
        this.caseDataStoreService = caseDataStoreService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity getDocumentMetadata(UUID documentId) {

        try {
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            LOG.error("Document Store URL is : " + documentURL);
            String documentMetadataUrl = String.format("%s/documents/%s", documentURL, documentId);
            LOG.error("documentMetadataUrl : " + documentMetadataUrl);
            ResponseEntity<StoredDocumentHalResource> response = restTemplate.exchange(
                documentMetadataUrl,
                GET,
                requestEntity,
                StoredDocumentHalResource.class
                                                                                      );
            LOG.error("response : " + response.getStatusCode());
            LOG.error("response : " + response.getBody());
            ResponseEntity responseEntity = ResponseHelper.toResponseEntity(response, documentId);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                LOG.error("Positive response");
                return responseEntity;
            } else {
                LOG.error("Document doesn't exist for requested document id at Document Store API Side " + responseEntity
                    .getStatusCode());
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
            Map<String, String> metadata = ((StoredDocumentHalResource) storedDocument).getMetadata();
            return metadata.get("caseId");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity<Object> getDocumentBinaryContent(UUID documentId) {
        try {
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            String documentBinaryUrl = String.format("%s/documents/%s/binary", documentURL, documentId);
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
    public ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification, List<String> roles,
                                                  String serviceAuthorization, String caseTypeId, String jurisdictionId,
                                                  String userId) {

        LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        HttpHeaders headers = prepareRequestForUpload(
            files,
            classification,
            roles,
            serviceAuthorization,
            caseTypeId,
            jurisdictionId,
            userId,
            bodyMap
                                                     );
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
        ResponseEntity<Object> uploadedDocumentResponse = restTemplate
            .postForEntity(documentURL.concat("/documents"), requestEntity, Object.class);

        if (HttpStatus.OK.equals(uploadedDocumentResponse.getStatusCode()) && null != uploadedDocumentResponse
            .getBody()) {
            formatUploadDocumentResponse(caseTypeId, jurisdictionId, uploadedDocumentResponse);
        }
        return ResponseEntity
            .status(uploadedDocumentResponse.getStatusCode())
            .body(uploadedDocumentResponse.getBody());
    }

    @SuppressWarnings("unchecked")
    private void formatUploadDocumentResponse(String caseTypeId, String jurisdictionId,
                                              ResponseEntity<Object> uploadedDocumentResponse) {
        try {
            LinkedHashMap documents = (LinkedHashMap) ((LinkedHashMap) uploadedDocumentResponse.getBody())
                .get(EMBEDDED);
            //LOG.error("Documents in response :" + documents);

            ArrayList<Object> documentList = (ArrayList<Object>) (documents.get(DOCUMENTS));
            LOG.error("documentList :" + documentList);

            for (Object document : documentList) {
                if (document instanceof LinkedHashMap) {
                    //LOG.error("Individual document :" + ((LinkedHashMap) document).entrySet());
                    LinkedHashMap<String, Object> hashmap = ((LinkedHashMap<String, Object>) (document));
                    hashmap.remove(EMBEDDED);
                    updateDomainForLinks(hashmap, jurisdictionId, caseTypeId);
                }
            }
        } catch (Exception exception) {
            LOG.error("Error while formatting the uploaded document response :" + exception);
            throw new ResponseFormatException("Error while formatting the uploaded document response " + exception);
        }
    }

    private void updateDomainForLinks(LinkedHashMap<String, Object> hashmap, String jurisdictionId, String caseTypeId) {
        try {
            JSONObject links = new JSONObject(hashmap).getJSONObject(LINKS);
            links.remove(THUMBNAIL);

            String href = (String) links.getJSONObject(SELF).get(HREF);
            links.getJSONObject(SELF).put(HREF, buildDocumentURL(href, 36));
            hashmap.put(HASHCODE, ApplicationUtils.generateHashCode(
                href.substring(href.length() - 36)
                    .concat(jurisdictionId)
                    .concat(caseTypeId)));

            links.getJSONObject(BINARY).put(HREF, buildDocumentURL((String) links.getJSONObject(BINARY).get(HREF), 43));
            hashmap.put(LINKS, links.toMap());
            LOG.error(hashmap.values().toString());
        } catch (Exception e) {
            LOG.error("Exception within UpdateDomainForLinks :" + e);
            throw e;
        }
    }

    private String buildDocumentURL(String documentUrl, int length) {
        documentUrl = documentUrl.substring(documentUrl.length() - length);
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();
        LOG.info("URL from request is: " + request.getRequestURL());
        return request.getRequestURL().append("/").append(documentUrl).toString();
    }

    private HttpHeaders prepareRequestForUpload(List<MultipartFile> files, String classification, List<String> roles,
                                                String serviceAuthorization,
                                                String caseTypeId, String jurisdictionId, String userId,
                                                LinkedMultiValueMap<String, Object> bodyMap) {
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
        //S2S token needs to be generated by our microservice.
        headers.set(SERVICE_AUTHORIZATION, serviceAuthorization);
        headers.set(USERID, userId);
        return headers;
    }

    private String getEffectiveTTL() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        return format.format(new Timestamp(new Date().getTime() + Long.parseLong(documentTtl)));
    }

    private HttpHeaders getHeaders(ResponseEntity<ByteArrayResource> response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ORIGINAL_FILE_NAME, response.getHeaders().get(ORIGINAL_FILE_NAME).get(0));
        headers.add(CONTENT_DISPOSITION, response.getHeaders().get(CONTENT_DISPOSITION).get(0));
        headers.add(DATA_SOURCE, response.getHeaders().get(DATA_SOURCE).get(0));
        headers.add(CONTENT_TYPE, response.getHeaders().get(CONTENT_TYPE).get(0));
        headers.add(CONTENT_LENGTH, response.getHeaders().get(CONTENT_LENGTH).get(0));
        return headers;

    }

    public boolean checkUserPermission(ResponseEntity responseEntity, UUID documentId, String authorization) {
        String caseId = extractCaseIdFromMetadata(responseEntity.getBody());

        if (!ValidationService.validate(caseId)) {
            LOG.error(CASE_ID_INVALID + HttpStatus.BAD_REQUEST);
            throw new BadRequestException(CASE_ID_INVALID);

        } else {
            CaseDocumentMetadata caseDocumentMetadata = caseDataStoreService
                .getCaseDocumentMetadata(caseId, documentId, authorization)
                .orElseThrow(() -> new CaseNotFoundException(caseId));

            return (caseDocumentMetadata.getDocument().getId().equals(documentId.toString())
                    && caseDocumentMetadata.getDocument().getPermissions().contains(Permission.READ));
        }
    }
}
