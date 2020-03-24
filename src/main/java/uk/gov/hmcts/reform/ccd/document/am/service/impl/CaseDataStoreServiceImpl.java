package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.util.SecurityUtils;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_INVALID;

@Slf4j
@Service
public class CaseDataStoreServiceImpl implements CaseDataStoreService {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDataStoreServiceImpl.class);

    @Value("${caseDataStoreUrl}")
    protected transient String caseDataStoreUrl;

    private transient RestTemplate restTemplate;
    private transient SecurityUtils securityUtils;


    @Autowired
    public CaseDataStoreServiceImpl(RestTemplate restTemplate, SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;
        this.securityUtils = securityUtils;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CaseDocumentMetadata> getCaseDocumentMetadata(String caseId, UUID documentId, String authorization) {
        try {
            HttpHeaders headers = prepareRequestForUpload(authorization);
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> responseEntity =
                restTemplate.exchange(caseDataStoreUrl
                                          .concat("/cases/")
                                          .concat(caseId)
                                          .concat("/documents/")
                                          .concat(documentId.toString()),
                                      HttpMethod.GET, requestEntity, Object.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK
                && responseEntity.getBody() instanceof LinkedHashMap) {
                LinkedHashMap<String, Object> responseObject = (LinkedHashMap<String, Object>) responseEntity.getBody();
                CaseDocumentMetadata caseDocumentMetadata = new ObjectMapper().convertValue(responseObject.get("documentMetadata"),
                                                                                            CaseDocumentMetadata.class);
                if (null == caseDocumentMetadata.getDocument()) {
                    LOG.error("Could't find document for case  : " + caseId + ", response code from CCD : " + HttpStatus.FORBIDDEN);
                    throw new ForbiddenException("Could't find document for case  : " + caseId);
                }
                return Optional.of(caseDocumentMetadata);
            }
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                LOG.error("Could't find document for case  : " + caseId + ", response code from CCD : " + HttpStatus.NOT_FOUND);
                throw new ForbiddenException("Could't find document for case  : " + caseId);
            } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
                LOG.error("Could't find document for case  : " + caseId + ", response code from CCD : " + HttpStatus.FORBIDDEN);
                throw new ForbiddenException("Could't find document for case  : " + caseId);
            } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
                LOG.error("Could't find document for case  : " + caseId + ", response code from CCD : " + HttpStatus.BAD_REQUEST);
                throw new BadRequestException(INPUT_INVALID);
            } else {
                LOG.error("Exception occurred while getting document permissions from CCD Data store:" + exception.getMessage());
                throw new ServiceException(String.format(
                    "Problem  fetching the document for document id: %s because of %s",
                    documentId,
                    exception.getMessage()
                ));
            }
        }
        return Optional.empty();
    }

    private HttpHeaders prepareRequestForUpload(String authorization) {

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(securityUtils.authorizationHeaders());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("experimental", "true");
        headers.set("Authorization", authorization);
        return headers;
    }
}
