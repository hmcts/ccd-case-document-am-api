package uk.gov.hmcts.reform.ccd.documentam.client.datastore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentResource;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class CaseDataStoreClientImpl implements CaseDataStoreClient {

    private static final String CASE_ERROR_MESSAGE = "Couldn't find document for case  : ";

    private final RestTemplate restTemplate;
    private final String caseDataStoreUrl;
    private final SecurityUtils securityUtils;

    @Autowired
    public CaseDataStoreClientImpl(@Qualifier("dataStoreRestTemplate") final RestTemplate restTemplate,
                                   @Value("${caseDataStoreUrl}") final String caseDataStoreUrl,
                                   final SecurityUtils securityUtils) {

        log.info("JCDEBUG: CaseDataStoreClientImpl: caseDataStoreUrl: {}", caseDataStoreUrl);

        this.restTemplate = restTemplate;
        this.caseDataStoreUrl = caseDataStoreUrl;
        this.securityUtils = securityUtils;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<DocumentPermissions> getCaseDocumentMetadata(String caseId, UUID documentId) {
        try {
            final HttpHeaders headers = prepareRequestForUpload();

            final ResponseEntity<CaseDocumentResource> responseEntity = restTemplate.exchange(
                String.format("%s/cases/%s/documents/%s", caseDataStoreUrl, caseId, documentId),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                CaseDocumentResource.class
            );

            return Optional.ofNullable(responseEntity.getBody())
                .map(CaseDocumentResource::getDocumentMetadata)
                .map(CaseDocumentMetadata::getDocumentPermissions);
        } catch (HttpClientErrorException exception) {
            log.error("Exception occurred while getting document permissions from CCD Data store: {}",
                      exception.getMessage());
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                return Optional.empty();
            } else if (HttpStatus.FORBIDDEN.equals(exception.getStatusCode())) {
                throw new ForbiddenException(CASE_ERROR_MESSAGE + caseId);
            } else if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
                throw new BadRequestException(Constants.INPUT_INVALID);
            } else {
                throw new ServiceException(String.format(
                    "Problem  fetching the document for document id: %s because of %s",
                    documentId,
                    exception.getMessage()
                ));
            }
        }
    }

    private HttpHeaders prepareRequestForUpload() {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(securityUtils.authorizationHeaders());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }
}
