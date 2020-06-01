package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import static javax.servlet.RequestDispatcher.ERROR_MESSAGE;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.BAD_REQUEST;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.BINARY;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CASE_ID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CLASSIFICATION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CREATED_BY;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DOCUMENTS;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.EMBEDDED;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.EXCEPTION_ERROR_MESSAGE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.EXCEPTION_SERVICE_ID_NOT_AUTHORISED;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.FILES;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.FORBIDDEN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.HASHTOKEN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.HREF;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_STRING_PATTERN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.JURISDICTION_ID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.LAST_MODIFIED_BY;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.LINKS;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.MODIFIED_ON;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ORIGINAL_FILE_NAME;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SELF;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.THUMBNAIL;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.USERID;

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
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
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
import uk.gov.hmcts.reform.ccd.document.am.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.document.am.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.document.am.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUpdate;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.service.ValidationUtils;
import uk.gov.hmcts.reform.ccd.document.am.util.ApplicationUtils;
import uk.gov.hmcts.reform.ccd.document.am.util.ResponseHelper;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

@Slf4j
@Service
public class DocumentManagementServiceImpl implements DocumentManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentManagementServiceImpl.class);

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
            LOG.info("services config loaded {}", authorisedServices);
        } catch (IOException e) {
            LOG.error("IOException {}", e.getMessage());
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
            ResponseEntity<Object> responseEntity = ResponseHelper.toResponseEntity(response, documentId);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                LOG.info("Positive response");
                responseResult = responseEntity;
            } else {
                LOG.error("Document doesn't exist for requested document id at Document Store {}", responseEntity
                    .getStatusCode());
                throw new ResourceNotFoundException(documentId.toString());
            }
        } catch (HttpClientErrorException exception) {
            catchException(exception, documentId.toString());
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
                responseResult =  ResponseEntity.ok().headers(getHeaders(response))
                                     .body(response.getBody());
            } else {
                responseResult = ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());
            }

        } catch (HttpClientErrorException exception) {
            catchException(exception, documentId.toString());
        }
        return responseResult;

    }

    @Override
    public ResponseEntity<Object> patchDocumentMetadata(CaseDocumentsMetadata caseDocumentsMetadata) {
        try {
            LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
            prepareRequestForAttachingDocumentToCase(caseDocumentsMetadata,  bodyMap);
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, getHttpHeaders());

            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            String documentUrl = String.format("%s/documents", documentURL);
            restTemplate.exchange(documentUrl, HttpMethod.PATCH, requestEntity, Void.class);

        } catch (HttpClientErrorException exception) {
            catchException(exception);
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
            metadataMap.put(CASE_ID, caseDocumentsMetadata.getCaseId());

            if (null != caseDocumentsMetadata.getCaseTypeId()) {
                validationUtils.validateInputParams(INPUT_STRING_PATTERN, caseDocumentsMetadata.getCaseTypeId());
                metadataMap.put(CASE_TYPE_ID, caseDocumentsMetadata.getCaseTypeId());
            }

            if (null != caseDocumentsMetadata.getJurisdictionId()) {
                validationUtils.validateInputParams(INPUT_STRING_PATTERN, caseDocumentsMetadata.getJurisdictionId());
                metadataMap.put(JURISDICTION_ID, caseDocumentsMetadata.getJurisdictionId());
            }

            DocumentUpdate documentUpdate = new DocumentUpdate();
            documentUpdate.setDocumentId(UUID.fromString(documentHashToken.getId()));

            documentUpdate.setMetadata(metadataMap);
            bodyMap.add("documents", documentUpdate);
        }
    }

    public String generateHashToken(UUID documentId) {
        ResponseEntity<?> responseEntity = getDocumentMetadata(documentId);
        String hashcodeFromStoredDocument = "";
        if (responseEntity.getStatusCode().equals(HttpStatus.OK) && null != responseEntity.getBody()) {
            StoredDocumentHalResource resource = (StoredDocumentHalResource) responseEntity.getBody();

            if (resource.getMetadata().get(CASE_ID) == null) {
                hashcodeFromStoredDocument = ApplicationUtils
                      .generateHashCode(salt.concat(documentId.toString()
                       .concat(resource.getMetadata().get(JURISDICTION_ID))
                       .concat(resource.getMetadata().get(CASE_TYPE_ID))));
            } else {
                hashcodeFromStoredDocument = ApplicationUtils
                    .generateHashCode(salt.concat(documentId.toString()
                      .concat(resource.getMetadata().get(CASE_ID))
                      .concat(resource.getMetadata().get(JURISDICTION_ID))
                      .concat(resource.getMetadata().get(CASE_TYPE_ID))));
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
                    bodyMap.add(FILES, file.getResource());
                }
            }
            String docUrl = String.format("%s/documents", documentURL);

            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
            LinkedHashMap<String, Object> updatedDocumentResponse = null;
            ResponseEntity<Object> uploadedDocumentResponse = restTemplate
                .postForEntity(docUrl, requestEntity, Object.class);

            if (HttpStatus.OK.equals(uploadedDocumentResponse.getStatusCode()) && null != uploadedDocumentResponse
                .getBody()) {
                updatedDocumentResponse = formatUploadDocumentResponse(caseTypeId, jurisdictionId, uploadedDocumentResponse);
            }

            responseResult = ResponseEntity
                .status(uploadedDocumentResponse.getStatusCode())
                .body(updatedDocumentResponse);
        } catch (HttpClientErrorException exception) {
            catchException(exception);
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
                LOG.error("Document doesn't exist for requested document id at Document Store API Side {}", response
                    .getStatusCode());
                throw new ResourceNotFoundException(documentId.toString());
            }
        } catch (HttpClientErrorException exception) {
            catchException(exception, documentId.toString());
        }
        return responseResult;
    }

    @Override
    public ResponseEntity<Object> deleteDocument(UUID documentId,  Boolean permanent) {
        ResponseEntity<Object> responseResult = new ResponseEntity<>(HttpStatus.OK);
        try {
            final HttpEntity<?> requestEntity = new HttpEntity<>(getHttpHeaders());
            String documentDeleteUrl = String.format("%s/documents/%s?permanent=%s", documentURL, documentId, permanent);
            LOG.info("documentDeleteUrl : {}", documentDeleteUrl);
            ResponseEntity<Object> response = restTemplate.exchange(
                documentDeleteUrl,
                DELETE,
                requestEntity,
                Object.class
            );
            if (HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
                LOG.info("Positive response");
                responseResult = response;
            } else {
                LOG.error("Document doesn't exist for requested document id at Document Store {}", response
                    .getStatusCode());
                throw new ResourceNotFoundException(documentId.toString());
            }
        } catch (HttpClientErrorException exception) {
            catchException(exception, documentId.toString());
        }
        return responseResult;
    }


    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> formatUploadDocumentResponse(String caseTypeId, String jurisdictionId,
                                                                       ResponseEntity<Object> uploadedDocumentResponse) {
        LinkedHashMap<String, Object> updatedUploadedDocumentResponse = new LinkedHashMap<>();
        try {
            LinkedHashMap<String, Object> documents = (LinkedHashMap) ((LinkedHashMap) uploadedDocumentResponse
                .getBody())
                .get(EMBEDDED);

            ArrayList<Object> documentList = (ArrayList<Object>) (documents.get(DOCUMENTS));
            LOG.info("documentList :{}", documentList);

            for (Object document : documentList) {
                if (document instanceof LinkedHashMap) {
                    LinkedHashMap<String, Object> hashmap = ((LinkedHashMap<String, Object>) (document));
                    hashmap.remove(EMBEDDED);
                    hashmap.remove(CREATED_BY);
                    hashmap.remove(LAST_MODIFIED_BY);
                    hashmap.remove(MODIFIED_ON);
                    updateDomainForLinks(hashmap, jurisdictionId, caseTypeId);
                }
            }
            ArrayList<Object> documentListObject =
                (ArrayList<Object>) ((LinkedHashMap) ((LinkedHashMap) uploadedDocumentResponse
                    .getBody())
                    .get(EMBEDDED)).get(DOCUMENTS);
            updatedUploadedDocumentResponse.put(DOCUMENTS, documentListObject);

            return updatedUploadedDocumentResponse;

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
        } catch (Exception exception) {
            LOG.error("Exception occurred {}", exception.toString());
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
        return headers;

    }

    public boolean checkUserPermission(ResponseEntity responseEntity, UUID documentId, Permission permissionToCheck) {
        String caseId = extractCaseIdFromMetadata(responseEntity.getBody());
        validationUtils.validate(caseId);

        DocumentPermissions documentPermissions = caseDataStoreService
            .getCaseDocumentMetadata(caseId, documentId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));

        return (documentPermissions.getId().equals(documentId.toString())
            && documentPermissions.getPermissions().contains(permissionToCheck));
    }

    public boolean checkServicePermission(ResponseEntity<?> responseEntity, Permission permission) {
        LOG.info("API call initiated from {} token ", securityUtils.getServiceId());
        AuthorisedService serviceConfig = getServiceDetailsFromJson(securityUtils.getServiceId());
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

    public boolean checkServicePermissionsForUpload(String caseTypeId, String jurisdictionId, Permission permission) {
        AuthorisedService serviceConfig = getServiceDetailsFromJson(securityUtils.getServiceId());
        return validateCaseTypeId(serviceConfig, caseTypeId) && validateJurisdictionId(
            serviceConfig,
            jurisdictionId
        ) && validatePermissions(
            serviceConfig,
            permission
        );
    }

    private boolean validateCaseTypeId(AuthorisedService serviceConfig, String caseTypeId) {
        boolean result = !StringUtils.isEmpty(caseTypeId) && (serviceConfig.getCaseTypeId().equals("*") || caseTypeId.equals(
            serviceConfig.getCaseTypeId()));
        caseTypeId = sanitiseData(caseTypeId);
        LOG.info("Case Type Id is {} and validation result is {}", caseTypeId, result);
        return result;
    }

    private boolean validateJurisdictionId(AuthorisedService serviceConfig, String jurisdictionId) {
        boolean result =  !StringUtils.isEmpty(jurisdictionId) && (serviceConfig.getJurisdictionId().equals("*") || jurisdictionId.equals(
            serviceConfig.getJurisdictionId()));
        jurisdictionId = sanitiseData(jurisdictionId);
        LOG.info("JurisdictionI Id is {} and validation result is {}", jurisdictionId, result);
        return result;
    }

    private String sanitiseData(String value) {
        return value.replaceAll("[\n|\r|\t]", "_");
    }

    private boolean validatePermissions(AuthorisedService serviceConfig, Permission permission) {
        List<Permission> servicePermissions = serviceConfig.getPermissions();
        boolean result = !servicePermissions.isEmpty() && (servicePermissions.contains(permission));
        LOG.info("Permission is {} and validation result is {}", permission, result);
        return result;
    }

    private AuthorisedService getServiceDetailsFromJson(String serviceId) {
        Optional<AuthorisedService> service = authorisedServices.getAuthServices().stream().filter(s -> s.getId().equals(
            serviceId)).findAny();
        if (service.isPresent()) {
            return service.get();
        } else {
            LOG.error("Service Id {} is not authorized to access API ", serviceId);
            throw new ForbiddenException(String.format(EXCEPTION_SERVICE_ID_NOT_AUTHORISED, serviceId));
        }
    }

    private String extractCaseTypeIdFromMetadata(Object storedDocument) {
        if (storedDocument instanceof StoredDocumentHalResource) {
            Map<String, String> metadata = ((StoredDocumentHalResource) storedDocument).getMetadata();
            return metadata.get(CASE_TYPE_ID);
        }
        return null;
    }

    private String extractJurisdictionIdFromMetadata(Object storedDocument) {
        if (storedDocument instanceof StoredDocumentHalResource) {
            Map<String, String> metadata = ((StoredDocumentHalResource) storedDocument).getMetadata();
            return metadata.get(JURISDICTION_ID);
        }
        return null;
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = securityUtils.serviceAuthorizationHeaders();
        headers.set(USERID, securityUtils.getUserId());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void catchException(HttpClientErrorException exception, String messageParam) {
        if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
            LOG.error(ERROR_MESSAGE + "{}" + "{}", messageParam, HttpStatus.NOT_FOUND);
            throw new ResourceNotFoundException(messageParam);
        } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
            LOG.error(ERROR_MESSAGE + "{} {}", messageParam, HttpStatus.FORBIDDEN);
            throw new ForbiddenException(messageParam);
        } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
            LOG.error(ERROR_MESSAGE + "{} {}", messageParam, HttpStatus.BAD_REQUEST);
            throw new BadRequestException(messageParam);
        } else {
            throw new ServiceException(String.format(
                EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE,
                messageParam,
                exception));
        }
    }

    private void catchException(HttpClientErrorException exception) {
        if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
            LOG.error(ERROR_MESSAGE + "{}", HttpStatus.NOT_FOUND);
            throw new ResourceNotFoundException(RESOURCE_NOT_FOUND);
        } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
            LOG.error(ERROR_MESSAGE + "{}", HttpStatus.FORBIDDEN);
            throw new ForbiddenException(FORBIDDEN);
        } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
            LOG.error(ERROR_MESSAGE + "{}", HttpStatus.BAD_REQUEST);
            throw new BadRequestException(BAD_REQUEST);
        } else {
            throw new ServiceException(String.format(
                EXCEPTION_ERROR_MESSAGE,
                exception));
        }
    }
}
