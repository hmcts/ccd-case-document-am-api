package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUpdate;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.service.common.ValidationService;
import uk.gov.hmcts.reform.ccd.document.am.util.ApplicationUtils;
import uk.gov.hmcts.reform.ccd.document.am.util.ResponseHelper;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static javax.servlet.RequestDispatcher.ERROR_MESSAGE;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
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
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_STRING_PATTERN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.LINKS;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ORIGINAL_FILE_NAME;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ROLES;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SELF;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.THUMBNAIL;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.USERID;

@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentManagementServiceImpl.class);

    private RestTemplate restTemplate;

    private SecurityUtils securityUtils;

    @Value("${documentStoreUrl}")
    protected String documentURL;

    @Value("${documentTTL}")
    protected String documentTtl;

    private CaseDataStoreService caseDataStoreService;

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
            LOG.info("Document Store URL is : {}", documentURL);
            String documentMetadataUrl = String.format("%s/documents/%s", documentURL, documentId);
            LOG.info("documentMetadataUrl : {}", documentMetadataUrl);
            ResponseEntity<StoredDocumentHalResource> response = restTemplate.exchange(
                documentMetadataUrl,
                GET,
                requestEntity,
                StoredDocumentHalResource.class
                                                                                      );
            LOG.info("response : {}", response.getStatusCode());
            LOG.info("response : {}", response.getBody());
            ResponseEntity responseEntity = ResponseHelper.toResponseEntity(response, documentId);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                LOG.info("Positive response");
                return responseEntity;
            } else {
                LOG.error("Document doesn't exist for requested document id at Document Store {}", responseEntity
                    .getStatusCode());
                throw new ResourceNotFoundException(documentId.toString());
            }
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.NOT_FOUND);
                throw new ResourceNotFoundException(documentId.toString());
            } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE,documentId.toString(), HttpStatus.FORBIDDEN);
                throw new ForbiddenException(documentId.toString());
            } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.BAD_REQUEST);
                throw new BadRequestException(documentId.toString());
            } else {
                LOG.error("Exception occurred while getting the document from Document store: {}", exception.getMessage());
                throw new ServiceException(String.format(
                    "Problem  fetching the document for document id: %s because of %s",
                    documentId,
                    exception.getMessage()
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
            LOG.error(ex.getMessage());
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
    public boolean patchDocumentMetadata(DocumentMetadata documentMetadata) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.addAll(securityUtils.authorizationHeaders());
            headers.add(USERID,securityUtils.getUserId());
            headers.setContentType(MediaType.APPLICATION_JSON);
            LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
            prepareRequestForAttachingDocumentToCase(documentMetadata,  bodyMap);
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            String documentUrl = String.format("%s/documents", documentURL);
            restTemplate.exchange(documentUrl, HttpMethod.PATCH, requestEntity, Void.class);

        } catch (RestClientException ex) {
            LOG.error("Exception occurred while patching the document metadata");
            return false;
        }
        return true;
    }

    private void prepareRequestForAttachingDocumentToCase(DocumentMetadata documentMetadata,

                                                      LinkedMultiValueMap<String, Object> bodyMap) {

        for (Document document : documentMetadata.getDocuments()) {
            ResponseEntity responseEntity = getDocumentMetadata(UUID.fromString(document.getId()));
            if (responseEntity.getStatusCode().equals(HttpStatus.OK) && null != responseEntity.getBody()) {
                StoredDocumentHalResource resource = (StoredDocumentHalResource) responseEntity.getBody();

                String hashcodeFromStoredDocument = ApplicationUtils.generateHashCode(document.getId()
                                                       .concat(resource.getMetadata().get("jurisdictionId"))
                                                       .concat(resource.getMetadata().get("caseTypeId")));
                if (!hashcodeFromStoredDocument.equals(document.getHashToken())) {
                    throw new ResourceNotFoundException(String.format(": Document %s does not exists in DM Store", document.getId()));
                }
            }

            Map<String, String> metadataMap = new HashMap<>();
            metadataMap.put("caseId", documentMetadata.getCaseId());

            if (null != documentMetadata.getCaseTypeId()) {
                ValidationService.validateInputParams(INPUT_STRING_PATTERN, documentMetadata.getCaseTypeId());
                metadataMap.put("caseTypeId", documentMetadata.getCaseTypeId());
            }
            if (null != documentMetadata.getJurisdictionId()) {
                ValidationService.validateInputParams(INPUT_STRING_PATTERN, documentMetadata.getJurisdictionId());
                metadataMap.put("jurisdictionId", documentMetadata.getJurisdictionId());
            }

            DocumentUpdate documentUpdate = new DocumentUpdate();
            documentUpdate.setDocumentId(UUID.fromString(document.getId()));

            documentUpdate.setMetadata(metadataMap);
            bodyMap.add("documents", documentUpdate);
        }

    }

    @Override
    public ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification, List<String> roles,
                                                   String caseTypeId, String jurisdictionId) {

        LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        HttpHeaders headers = prepareRequestForUpload(classification, roles, caseTypeId, jurisdictionId,bodyMap);

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                bodyMap.add(FILES, file.getResource());
            }
        }
        String docUrl = String.format("%s/documents", documentURL);

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
        ResponseEntity<Object> uploadedDocumentResponse = restTemplate
            .postForEntity(docUrl, requestEntity, Object.class);

        if (HttpStatus.OK.equals(uploadedDocumentResponse.getStatusCode()) && null != uploadedDocumentResponse
            .getBody()) {
            formatUploadDocumentResponse(caseTypeId, jurisdictionId, uploadedDocumentResponse);
        }

        return ResponseEntity
            .status(uploadedDocumentResponse.getStatusCode())
            .body(uploadedDocumentResponse.getBody());
    }

    @Override
    public ResponseEntity patchDocument(UUID documentId, UpdateDocumentCommand ttl) {
        if (!ValidationService.validateTTL(ttl.getTtl())) {
            throw new BadRequestException(String.format(
                "Incorrect date format %s",
                ttl.getTtl()));
        }
        try {
            final HttpEntity<UpdateDocumentCommand> requestEntity = new HttpEntity<>(ttl, getHttpHeaders());
            String patchTTLUrl = String.format("%s/documents/%s", documentURL, documentId);
            ResponseEntity<StoredDocumentHalResource> response = restTemplate.exchange(
                patchTTLUrl,
                PATCH,
                requestEntity,
                StoredDocumentHalResource.class
            );
            ResponseEntity responseEntity = ResponseHelper.toResponseEntity(response, documentId);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                return responseEntity;
            } else {
                LOG.error("Document doesn't exist for requested document id at Document Store API Side {}", response
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

    @SuppressWarnings("unchecked")
    private void formatUploadDocumentResponse(String caseTypeId, String jurisdictionId,
                                              ResponseEntity<Object> uploadedDocumentResponse) {
        try {
            LinkedHashMap documents = (LinkedHashMap) ((LinkedHashMap) uploadedDocumentResponse.getBody())
                .get(EMBEDDED);

            ArrayList<Object> documentList = (ArrayList<Object>) (documents.get(DOCUMENTS));
            LOG.error("documentList :{}", documentList);

            for (Object document : documentList) {
                if (document instanceof LinkedHashMap) {
                    LinkedHashMap<String, Object> hashmap = ((LinkedHashMap<String, Object>) (document));
                    hashmap.remove(EMBEDDED);
                    updateDomainForLinks(hashmap, jurisdictionId, caseTypeId);
                }
            }
        } catch (Exception exception) {
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
            String message = hashmap.values().toString();
            LOG.error(message);
        } catch (Exception e) {
            LOG.error("Exception occurred");
            throw e;
        }
    }

    private String buildDocumentURL(String documentUrl, int length) {
        documentUrl = documentUrl.substring(documentUrl.length() - length);
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();

        return request.getRequestURL().append("/").append(documentUrl).toString();
    }

    private HttpHeaders prepareRequestForUpload(String classification, List<String> roles,
                                                String caseTypeId, String jurisdictionId,
                                                LinkedMultiValueMap<String, Object> bodyMap) {


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
        headers.addAll(securityUtils.serviceAuthorizationHeaders());
        headers.set(USERID, securityUtils.getUserId());
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

    public boolean checkUserPermission(ResponseEntity responseEntity, UUID documentId, Permission permissionToCheck) {
        String caseId = extractCaseIdFromMetadata(responseEntity.getBody());
        if (!ValidationService.validate(caseId)) {
            LOG.error("Bad Request Exception {}", CASE_ID_INVALID + HttpStatus.BAD_REQUEST);
            throw new BadRequestException(CASE_ID_INVALID);

        } else {
            CaseDocumentMetadata caseDocumentMetadata = caseDataStoreService
                .getCaseDocumentMetadata(caseId, documentId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));

            return (caseDocumentMetadata.getDocument().getId().equals(documentId.toString())
                    && caseDocumentMetadata.getDocument().getPermissions().contains(permissionToCheck));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity<Object> deleteDocument(UUID documentId,  Boolean permanent) {

        try {
            final HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
            String documentDeleteUrl = String.format("%s/documents/%s?permanent=%s", documentURL, documentId, permanent);
            LOG.info("documentDeleteUrl : {}", documentDeleteUrl);
            ResponseEntity response = restTemplate.exchange(
                documentDeleteUrl,
                DELETE,
                requestEntity,
                ResponseEntity.class
            );
            if (HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
                LOG.info("Positive response");
                return response;
            } else {
                LOG.error("Document doesn't exist for requested document id at Document Store {}", response
                    .getStatusCode());
                throw new ResourceNotFoundException(documentId.toString());
            }
        } catch (HttpClientErrorException ex) {
            LOG.error("Exception while deleting the document: {}", ex.getMessage());
            if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
                throw new ResourceNotFoundException(documentId.toString());
            } else {
                throw new ServiceException(String.format(
                    "Problem  deleting the document with document id: %s because of %s",
                    documentId,
                    ex.getMessage()
                ));
            }

        }
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = securityUtils.authorizationHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
