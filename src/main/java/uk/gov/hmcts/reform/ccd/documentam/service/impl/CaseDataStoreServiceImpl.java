package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.CaseDataStoreService;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class CaseDataStoreServiceImpl implements CaseDataStoreService {

    private static final String ERROR_MESSAGE = "Couldn't find document for case  : {}, response code from CCD : {}";
    private static final String CASE_ERROR_MESSAGE = "Couldn't find document for case  : ";

    private final RestTemplate restTemplate;
    private final String caseDataStoreUrl;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    @Autowired
    public CaseDataStoreServiceImpl(final RestTemplate restTemplate,
                                    @Value("${caseDataStoreUrl}") final String caseDataStoreUrl,
                                    final SecurityUtils securityUtils,
                                    final ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.caseDataStoreUrl = caseDataStoreUrl;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<DocumentPermissions> getCaseDocumentMetadata(String caseId, UUID documentId) {
        Optional<DocumentPermissions> result = Optional.empty();
        try {
            HttpHeaders headers = prepareRequestForUpload();
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
            String documentUrl = String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, caseId, documentId);

            ResponseEntity<Object> responseEntity =
                restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, Object.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK
                && responseEntity.getBody() instanceof LinkedHashMap) {

                final Optional<CaseDocumentMetadata> documentMetadata = Optional.ofNullable(responseEntity.getBody())
                    .map(body -> {
                        final LinkedHashMap<String, Object> responseObject = (LinkedHashMap<String, Object>) body;
                        return objectMapper.convertValue(
                            responseObject.get("documentMetadata"),
                            CaseDocumentMetadata.class
                        );
                    });

                final DocumentPermissions documentPermissions = documentMetadata
                    .map(CaseDocumentMetadata::getDocumentPermissions)
                    .orElseThrow(() -> {
                        log.error(ERROR_MESSAGE, caseId, HttpStatus.FORBIDDEN);
                        throw new ForbiddenException(CASE_ERROR_MESSAGE + caseId);
                    });

                result = Optional.of(documentPermissions);
            }
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                log.error(ERROR_MESSAGE, caseId, HttpStatus.NOT_FOUND);
                throw new ForbiddenException(CASE_ERROR_MESSAGE + caseId);
            } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
                log.error(ERROR_MESSAGE, caseId, HttpStatus.FORBIDDEN);
                throw new ForbiddenException(CASE_ERROR_MESSAGE + caseId);
            } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
                log.error(ERROR_MESSAGE, caseId, HttpStatus.BAD_REQUEST);
                throw new BadRequestException(Constants.INPUT_INVALID);
            } else {
                log.error("Exception occurred while getting document permissions from CCD Data store: {}",
                    exception.getMessage());
                throw new ServiceException(String.format(
                    "Problem  fetching the document for document id: %s because of %s",
                    documentId,
                    exception.getMessage()
                ));
            }
        }
        return result;
    }

    private HttpHeaders prepareRequestForUpload() {

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(securityUtils.authorizationHeaders());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("experimental", "true");
        return headers;
    }
}
