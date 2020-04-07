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
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.BAD_REQUEST;
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
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.FORBIDDEN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.HASHTOKEN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.HREF;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_STRING_PATTERN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.LINKS;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ORIGINAL_FILE_NAME;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.RESOURCE_NOT_FOUND;
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

    @Value("${idam.s2s-auth.totp_secret}")
    protected String salt;

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
            final HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
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
            final HttpEntity requestEntity = new HttpEntity(getHttpHeaders());
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

        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.NOT_FOUND);
                throw new ResourceNotFoundException(documentId.toString());
            } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.FORBIDDEN);
                throw new ForbiddenException(documentId.toString());
            } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.BAD_REQUEST);
                throw new BadRequestException(documentId.toString());
            } else {
                LOG.error(
                    "Exception occurred while getting the document from Document store: {}",
                    exception.getMessage()
                );
                throw new ServiceException(String.format(
                    "Problem  fetching the document for document id: %s because of %s",
                    documentId,
                    exception.getMessage()
                ));
            }

        }

    }

    @Override
    public boolean patchDocumentMetadata(DocumentMetadata documentMetadata) {
        try {
            LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
            prepareRequestForAttachingDocumentToCase(documentMetadata,  bodyMap);
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, getHttpHeaders());

            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            String documentUrl = String.format("%s/documents", documentURL);
            restTemplate.exchange(documentUrl, HttpMethod.PATCH, requestEntity, Void.class);

        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, HttpStatus.NOT_FOUND);
                throw new ResourceNotFoundException(RESOURCE_NOT_FOUND);
            } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, HttpStatus.FORBIDDEN);
                throw new ForbiddenException(FORBIDDEN);
            } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, HttpStatus.BAD_REQUEST);
                throw new BadRequestException(BAD_REQUEST);
            } else {
                LOG.error(
                    "Exception occurred while performing the operation at Document store: {}",
                    exception.getMessage()
                );
                throw new ServiceException(String.format(
                    "Problem  performing the operation because of %s",
                    exception.getMessage()
                ));
            }
        }
        return true;
    }

    private void prepareRequestForAttachingDocumentToCase(DocumentMetadata documentMetadata,

                                                      LinkedMultiValueMap<String, Object> bodyMap) {

        for (Document document : documentMetadata.getDocuments()) {
            ResponseEntity responseEntity = getDocumentMetadata(UUID.fromString(document.getId()));
            if (responseEntity.getStatusCode().equals(HttpStatus.OK) && null != responseEntity.getBody()) {
                StoredDocumentHalResource resource = (StoredDocumentHalResource) responseEntity.getBody();

                String hashcodeFromStoredDocument = ApplicationUtils.generateHashCode(salt.concat(document.getId()
                                                       .concat(resource.getMetadata().get("jurisdictionId"))
                                                       .concat(resource.getMetadata().get("caseTypeId"))));
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
    public ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification,
                                                   String caseTypeId, String jurisdictionId) {
        try {
            LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
            HttpHeaders headers = prepareRequestForUpload(classification, caseTypeId, jurisdictionId, bodyMap);

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
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, HttpStatus.NOT_FOUND);
                throw new ResourceNotFoundException(RESOURCE_NOT_FOUND);
            } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, HttpStatus.FORBIDDEN);
                throw new ForbiddenException(FORBIDDEN);
            } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, HttpStatus.BAD_REQUEST);
                throw new BadRequestException(BAD_REQUEST);
            } else {
                LOG.error(
                    "Exception occurred while performing the operation at Document store: {}",
                    exception.getMessage()
                );
                throw new ServiceException(String.format(
                    "Problem  performing the operation because of %s",
                    exception.getMessage()
                ));
            }
        }
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
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.NOT_FOUND);
                throw new ResourceNotFoundException(documentId.toString());
            } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.FORBIDDEN);
                throw new ForbiddenException(documentId.toString());
            } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.BAD_REQUEST);
                throw new BadRequestException(documentId.toString());
            } else {
                LOG.error(
                    "Exception occurred while updating the document from Document store: {}",
                    exception.getMessage()
                );
                throw new ServiceException(String.format(
                    "Problem updating the document with document id: %s because of %s",
                    documentId,
                    exception.getMessage()
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
            LOG.info("documentList :{}", documentList);

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
            hashmap.put(HASHTOKEN, ApplicationUtils.generateHashCode(salt.concat(
                href.substring(href.length() - 36)
                    .concat(jurisdictionId)
                    .concat(caseTypeId))));

            links.getJSONObject(BINARY).put(HREF, buildDocumentURL((String) links.getJSONObject(BINARY).get(HREF), 43));
            hashmap.put(LINKS, links.toMap());
            String message = hashmap.values().toString();
            LOG.info(message);
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

    private HttpHeaders prepareRequestForUpload(String classification,
                                                String caseTypeId, String jurisdictionId,
                                                LinkedMultiValueMap<String, Object> bodyMap) {


        bodyMap.set(CLASSIFICATION, classification);
        bodyMap.set("metadata[jurisdictionId]", jurisdictionId);
        bodyMap.set("metadata[caseTypeId]", caseTypeId);
        bodyMap.set("ttl", getEffectiveTTL());

        HttpHeaders headers = getHttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
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
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.NOT_FOUND);
                throw new ResourceNotFoundException(documentId.toString());
            } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.FORBIDDEN);
                throw new ForbiddenException(documentId.toString());
            } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
                LOG.error(ERROR_MESSAGE, documentId.toString(), HttpStatus.BAD_REQUEST);
                throw new BadRequestException(documentId.toString());
            } else {
                LOG.error(
                    "Exception occurred while deleting the document from Document store: {}",
                    exception.getMessage()
                );
                throw new ServiceException(String.format(
                    "Problem  deleting the document with document id: %s because of %s",
                    documentId,
                    exception.getMessage()
                ));
            }

        }
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = securityUtils.serviceAuthorizationHeaders();
        headers.set(USERID, securityUtils.getUserId());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
