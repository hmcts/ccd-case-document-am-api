package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentUpdate;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.documentam.service.ValidationUtils;
import uk.gov.hmcts.reform.ccd.documentam.util.ApplicationUtils;
import uk.gov.hmcts.reform.ccd.documentam.util.ResponseHelper;

@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private final RestTemplate restTemplate;
    private final ValidationUtils validationUtils;
    private final SecurityUtils securityUtils;

    @Value("${documentStoreUrl}")
    protected String documentURL;
    @Value("${documentTTL}")
    protected String documentTtl;

    private final CaseDataStoreService caseDataStoreService;

    @Value("${idam.s2s-auth.totp_secret}")
    protected String salt;

    private static AuthorisedServices authorisedServices;

    static {
        InputStream inputStream = DocumentManagementServiceImpl.class.getClassLoader()
            .getResourceAsStream("service_config.json");
        try {
            authorisedServices = new ObjectMapper().readValue(inputStream, AuthorisedServices.class);
            log.info("services config loaded {}", authorisedServices);
        } catch (IOException e) {
            log.error("IOException {}", e.getMessage());
        }
    }

    @Autowired
    public DocumentManagementServiceImpl(RestTemplate restTemplate, SecurityUtils securityUtils,
                                         CaseDataStoreService caseDataStoreService,
                                         ValidationUtils validationUtils) {
        this.restTemplate = restTemplate;

        this.securityUtils = securityUtils;
        this.caseDataStoreService = caseDataStoreService;
        this.validationUtils = validationUtils;
    }

    @Override
    public ResponseEntity<Object> getDocumentMetadata(UUID documentId) {
        ResponseEntity<Object> responseResult = new ResponseEntity<>(HttpStatus.OK);
        try {
            final HttpEntity<String> requestEntity = new HttpEntity<>(getHttpHeaders());
            log.info("Document Store URL is : {}", documentURL);
            String documentMetadataUrl = String.format("%s/documents/%s", documentURL, documentId);
            log.info("documentMetadataUrl : {}", documentMetadataUrl);
            ResponseEntity<StoredDocumentHalResource> response = restTemplate.exchange(
                documentMetadataUrl,
                GET,
                requestEntity,
                StoredDocumentHalResource.class
            );
            log.info("response : {}", response.getStatusCode());
            log.info("response : {}", response.getBody());
            ResponseEntity<Object> responseEntity = ResponseHelper.toResponseEntity(response, documentId);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                log.info("Positive response");
                responseResult = responseEntity;
            } else {
                log.error("Document doesn't exist for requested document id at Document Store {}", responseEntity
                    .getStatusCode());
                throw new ResourceNotFoundException(formatNotFoundMessage(documentId.toString()));
            }
        } catch (HttpClientErrorException exception) {
            handleException(exception, documentId.toString());
        }
        return responseResult;

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
        ResponseEntity<Object> responseResult = new ResponseEntity<>(HttpStatus.OK);
        try {
            final HttpEntity<?> requestEntity = new HttpEntity<>(getHttpHeaders());
            String documentBinaryUrl = String.format("%s/documents/%s/binary", documentURL, documentId);
            ResponseEntity<ByteArrayResource> response = restTemplate.exchange(
                documentBinaryUrl,
                GET,
                requestEntity,
                ByteArrayResource.class
            );
            if (HttpStatus.OK.equals(response.getStatusCode())) {
                responseResult = ResponseEntity.ok().headers(getHeaders(response))
                    .body(response.getBody());
            } else {
                responseResult = ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());
            }

        } catch (HttpClientErrorException exception) {
            handleException(exception, documentId.toString());
        }
        return responseResult;

    }

    @Override
    public ResponseEntity<Object> patchDocumentMetadata(CaseDocumentsMetadata caseDocumentsMetadata) {
        try {
            LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
            prepareRequestForAttachingDocumentToCase(caseDocumentsMetadata, bodyMap);
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, getHttpHeaders());

            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            String documentUrl = String.format("%s/documents", documentURL);
            restTemplate.exchange(documentUrl, HttpMethod.PATCH, requestEntity, Void.class);

        } catch (HttpClientErrorException exception) {
            handleException(exception);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void prepareRequestForAttachingDocumentToCase(CaseDocumentsMetadata caseDocumentsMetadata,
                                                          LinkedMultiValueMap<String, Object> bodyMap) {

        for (DocumentHashToken documentHashToken : caseDocumentsMetadata.getDocumentHashTokens()) {

            String hashcodeFromStoredDocument = generateHashToken(UUID.fromString(documentHashToken.getId()));
            if (!hashcodeFromStoredDocument.equals(documentHashToken.getHashToken())) {
                throw new ForbiddenException(UUID.fromString(documentHashToken.getId()));
            }

            Map<String, String> metadataMap = new HashMap<>();
            metadataMap.put(Constants.CASE_ID, caseDocumentsMetadata.getCaseId());

            if (caseDocumentsMetadata.getCaseTypeId() != null) {
                validationUtils.validateInputParams(Constants.INPUT_STRING_PATTERN,
                    caseDocumentsMetadata.getCaseTypeId());
                metadataMap.put(Constants.CASE_TYPE_ID, caseDocumentsMetadata.getCaseTypeId());
            }

            if (caseDocumentsMetadata.getJurisdictionId() != null) {
                validationUtils.validateInputParams(Constants.INPUT_STRING_PATTERN,
                    caseDocumentsMetadata.getJurisdictionId());
                metadataMap.put(Constants.JURISDICTION_ID, caseDocumentsMetadata.getJurisdictionId());
            }

            DocumentUpdate documentUpdate = new DocumentUpdate();
            documentUpdate.setDocumentId(UUID.fromString(documentHashToken.getId()));

            documentUpdate.setMetadata(metadataMap);
            bodyMap.add("documents", documentUpdate);
        }
    }

    @Override
    public String generateHashToken(UUID documentId) {
        ResponseEntity<?> responseEntity = getDocumentMetadata(documentId);
        String hashcodeFromStoredDocument = "";
        if (responseEntity.getStatusCode().equals(HttpStatus.OK) && responseEntity.getBody() != null) {
            StoredDocumentHalResource resource = (StoredDocumentHalResource) responseEntity.getBody();

            if (resource.getMetadata().get(Constants.CASE_ID) == null) {
                hashcodeFromStoredDocument = ApplicationUtils
                    .generateHashCode(salt.concat(documentId.toString()
                        .concat(resource.getMetadata().get(Constants.JURISDICTION_ID))
                        .concat(resource.getMetadata().get(Constants.CASE_TYPE_ID))));
            } else {
                hashcodeFromStoredDocument = ApplicationUtils
                    .generateHashCode(salt.concat(documentId.toString()
                        .concat(resource.getMetadata().get(Constants.CASE_ID))
                        .concat(resource.getMetadata().get(Constants.JURISDICTION_ID))
                        .concat(resource.getMetadata().get(Constants.CASE_TYPE_ID))));
            }
        }
        return hashcodeFromStoredDocument;
    }

    @Override
    public ResponseEntity<Object> uploadDocuments(List<MultipartFile> files, String classification,
                                                  String caseTypeId, String jurisdictionId) {
        ResponseEntity<Object> responseResult = new ResponseEntity<>(HttpStatus.OK);
        try {
            LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
            HttpHeaders headers = prepareRequestForUpload(classification, caseTypeId, jurisdictionId, bodyMap);

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    bodyMap.add(Constants.FILES, file.getResource());
                }
            }
            String docUrl = String.format("%s/documents", documentURL);

            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
            LinkedHashMap<String, Object> updatedDocumentResponse = null;
            ResponseEntity<Object> uploadedDocumentResponse = restTemplate
                .postForEntity(docUrl, requestEntity, Object.class);

            if (HttpStatus.OK.equals(uploadedDocumentResponse.getStatusCode())
                && uploadedDocumentResponse.getBody() != null) {
                updatedDocumentResponse = formatUploadDocumentResponse(caseTypeId, jurisdictionId,
                    uploadedDocumentResponse);
            }

            responseResult = ResponseEntity
                .status(uploadedDocumentResponse.getStatusCode())
                .body(updatedDocumentResponse);
        } catch (HttpClientErrorException exception) {
            handleException(exception);
        }
        return responseResult;
    }

    @Override
    public ResponseEntity<Object> patchDocument(UUID documentId, UpdateDocumentCommand ttl) {
        ResponseEntity<Object> responseResult = new ResponseEntity<>(HttpStatus.OK);
        if (!validationUtils.validateTTL(ttl.getTtl())) {
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
            ResponseEntity<Object> responseEntity = ResponseHelper.updatePatchTTLResponse(response);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                responseResult = responseEntity;
            } else {
                log.error("Document doesn't exist for requested document id at Document Store API Side {}", response
                    .getStatusCode());
                throw new ResourceNotFoundException(formatNotFoundMessage(documentId.toString()));
            }
        } catch (HttpClientErrorException exception) {
            handleException(exception, documentId.toString());
        }
        return responseResult;
    }

    @Override
    public ResponseEntity<Object> deleteDocument(UUID documentId, Boolean permanent) {
        ResponseEntity<Object> responseResult = new ResponseEntity<>(HttpStatus.OK);
        try {
            final HttpEntity<?> requestEntity = new HttpEntity<>(getHttpHeaders());
            String documentDeleteUrl = String.format("%s/documents/%s?permanent=%s", documentURL, documentId,
                permanent);
            log.info("documentDeleteUrl : {}", documentDeleteUrl);
            ResponseEntity<Object> response = restTemplate.exchange(
                documentDeleteUrl,
                DELETE,
                requestEntity,
                Object.class
            );
            if (HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
                log.info("Positive response");
                responseResult = response;
            } else {
                log.error("Document doesn't exist for requested document id at Document Store {}", response
                    .getStatusCode());
                throw new ResourceNotFoundException(formatNotFoundMessage(documentId.toString()));
            }
        } catch (HttpClientErrorException exception) {
            handleException(exception, documentId.toString());
        }
        return responseResult;
    }


    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> formatUploadDocumentResponse(
        String caseTypeId, String jurisdictionId, ResponseEntity<Object> uploadedDocumentResponse) {
        LinkedHashMap<String, Object> updatedUploadedDocumentResponse = new LinkedHashMap<>();
        try {
            LinkedHashMap<String, Object> documents =
                (LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) uploadedDocumentResponse
                    .getBody())
                    .get(Constants.EMBEDDED);

            ArrayList<Object> documentList = (ArrayList<Object>) (documents.get(Constants.DOCUMENTS));
            log.info("documentList :{}", documentList);

            for (Object document : documentList) {
                if (document instanceof LinkedHashMap) {
                    LinkedHashMap<String, Object> hashmap = ((LinkedHashMap<String, Object>) (document));
                    hashmap.remove(Constants.EMBEDDED);
                    hashmap.remove(Constants.CREATED_BY);
                    hashmap.remove(Constants.LAST_MODIFIED_BY);
                    hashmap.remove(Constants.MODIFIED_ON);
                    updateDomainForLinks(hashmap, jurisdictionId, caseTypeId);
                }
            }
            ArrayList<Object> documentListObject =
                (ArrayList<Object>) ((LinkedHashMap<String, Object>)
                    ((LinkedHashMap<String, Object>) uploadedDocumentResponse
                    .getBody())
                    .get(Constants.EMBEDDED)).get(Constants.DOCUMENTS);
            updatedUploadedDocumentResponse.put(Constants.DOCUMENTS, documentListObject);

            return updatedUploadedDocumentResponse;

        } catch (Exception exception) {
            throw new ResponseFormatException("Error while formatting the uploaded document response " + exception);
        }
    }

    private void updateDomainForLinks(LinkedHashMap<String, Object> hashmap, String jurisdictionId, String caseTypeId) {
        try {
            JSONObject links = new JSONObject(hashmap).getJSONObject(Constants.LINKS);
            links.remove(Constants.THUMBNAIL);

            String href = (String) links.getJSONObject(Constants.SELF).get(Constants.HREF);
            links.getJSONObject(Constants.SELF).put(Constants.HREF, buildDocumentURL(href, 36));
            hashmap.put(Constants.HASHTOKEN, ApplicationUtils.generateHashCode(salt.concat(
                href.substring(href.length() - 36)
                    .concat(jurisdictionId)
                    .concat(caseTypeId))));

            links.getJSONObject(Constants.BINARY).put(Constants.HREF, buildDocumentURL((String) links.getJSONObject(
                Constants.BINARY).get(Constants.HREF), 43));
            hashmap.put(Constants.LINKS, links.toMap());
            String message = hashmap.values().toString();
            log.info(message);
        } catch (Exception exception) {
            log.error("Exception occurred", exception);
            throw exception;
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
        bodyMap.set(Constants.CLASSIFICATION, classification);
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
        headers.add(Constants.ORIGINAL_FILE_NAME, response.getHeaders().get(Constants.ORIGINAL_FILE_NAME).get(0));
        headers.add(Constants.CONTENT_DISPOSITION, response.getHeaders().get(Constants.CONTENT_DISPOSITION).get(0));
        headers.add(Constants.DATA_SOURCE, response.getHeaders().get(Constants.DATA_SOURCE).get(0));
        headers.add(Constants.CONTENT_TYPE, response.getHeaders().get(Constants.CONTENT_TYPE).get(0));
        return headers;

    }

    @Override
    public boolean checkUserPermission(ResponseEntity<?> responseEntity, UUID documentId,
                                       Permission permissionToCheck) {
        String caseId = extractCaseIdFromMetadata(responseEntity.getBody());
        validationUtils.validate(caseId);

        DocumentPermissions documentPermissions = caseDataStoreService
            .getCaseDocumentMetadata(caseId, documentId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));

        return (documentPermissions.getId().equals(documentId.toString())
            && documentPermissions.getPermissions().contains(permissionToCheck));
    }

    @Override
    public boolean checkServicePermission(ResponseEntity<?> responseEntity, String serviceId, Permission permission) {
        AuthorisedService serviceConfig = getServiceDetailsFromJson(serviceId);
        String caseTypeId = extractCaseTypeIdFromMetadata(responseEntity.getBody());
        String jurisdictionId = extractJurisdictionIdFromMetadata(responseEntity.getBody());
        return validateCaseTypeId(serviceConfig, caseTypeId) && validateJurisdictionId(
            serviceConfig,
            jurisdictionId
        ) && validatePermissions(
            serviceConfig,
            permission
        );
    }

    @Override
    public boolean checkServicePermissionsForUpload(String caseTypeId, String jurisdictionId,
                                                    String serviceId, Permission permission) {
        AuthorisedService serviceConfig = getServiceDetailsFromJson(serviceId);
        return validateCaseTypeId(serviceConfig, caseTypeId) && validateJurisdictionId(
            serviceConfig,
            jurisdictionId
        ) && validatePermissions(
            serviceConfig,
            permission
        );
    }

    private boolean validateCaseTypeId(AuthorisedService serviceConfig, String caseTypeId) {
        boolean result =
            !StringUtils.isEmpty(caseTypeId) && (serviceConfig.getCaseTypeId().equals("*") || caseTypeId.equals(
                serviceConfig.getCaseTypeId()));
        caseTypeId = sanitiseData(caseTypeId);
        log.info("Case Type Id is {} and validation result is {}", caseTypeId, result);
        return result;
    }

    private boolean validateJurisdictionId(AuthorisedService serviceConfig, String jurisdictionId) {
        boolean result =
            !StringUtils.isEmpty(jurisdictionId) && (serviceConfig.getJurisdictionId().equals("*")
                || jurisdictionId.equals(
                serviceConfig.getJurisdictionId()));
        jurisdictionId = sanitiseData(jurisdictionId);
        log.info("JurisdictionI Id is {} and validation result is {}", jurisdictionId, result);
        return result;
    }

    private String sanitiseData(String value) {
        return value.replaceAll("[\n|\r|\t]", "_");
    }

    private boolean validatePermissions(AuthorisedService serviceConfig, Permission permission) {
        List<Permission> servicePermissions = serviceConfig.getPermissions();
        boolean result = !servicePermissions.isEmpty() && (servicePermissions.contains(permission));
        log.info("Permission is {} and validation result is {}", permission, result);
        return result;
    }

    private AuthorisedService getServiceDetailsFromJson(String serviceId) {
        Optional<AuthorisedService> service =
            authorisedServices.getAuthServices().stream().filter(s -> s.getId().equals(
                serviceId)).findAny();
        if (service.isPresent()) {
            return service.get();
        } else {
            log.error("Service Id {} is not authorized to access API ", serviceId);
            throw new ForbiddenException(String.format(Constants.EXCEPTION_SERVICE_ID_NOT_AUTHORISED, serviceId));
        }
    }

    private String extractCaseTypeIdFromMetadata(Object storedDocument) {
        if (storedDocument instanceof StoredDocumentHalResource) {
            Map<String, String> metadata = ((StoredDocumentHalResource) storedDocument).getMetadata();
            return metadata.get(Constants.CASE_TYPE_ID);
        }
        return null;
    }

    private String extractJurisdictionIdFromMetadata(Object storedDocument) {
        if (storedDocument instanceof StoredDocumentHalResource) {
            Map<String, String> metadata = ((StoredDocumentHalResource) storedDocument).getMetadata();
            return metadata.get(Constants.JURISDICTION_ID);
        }
        return null;
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = securityUtils.serviceAuthorizationHeaders();
        headers.set(Constants.USERID, securityUtils.getUserInfo().getUid());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void handleException(HttpClientErrorException exception, String messageParam) {
        if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
            throw new ResourceNotFoundException(formatNotFoundMessage(messageParam), exception);
        } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
            throw new ForbiddenException(messageParam, exception);
        } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
            throw new BadRequestException(messageParam, exception);
        } else {
            throw new ServiceException(String.format(Constants.EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE, messageParam),
                exception);
        }
    }

    private void handleException(HttpClientErrorException exception) {
        if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
            throw new ResourceNotFoundException(Constants.RESOURCE_NOT_FOUND, exception);
        } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
            throw new ForbiddenException(Constants.FORBIDDEN, exception);
        } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
            throw new BadRequestException(Constants.BAD_REQUEST, exception);
        } else {
            throw new ServiceException(Constants.EXCEPTION_ERROR_MESSAGE, exception);
        }
    }

    private String formatNotFoundMessage(String resourceId) {
        return Constants.RESOURCE_NOT_FOUND + " " + resourceId;
    }
}
