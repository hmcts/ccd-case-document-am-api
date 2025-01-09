package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;

import java.net.SocketTimeoutException;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.springframework.http.HttpMethod.GET;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DM_DATE_TIME_FORMATTER;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DOCUMENT_METADATA_NOT_FOUND;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.RESOURCE_NOT_FOUND;

@Component
@Slf4j
public class DocumentStoreClient {

    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;


    @Autowired
    public DocumentStoreClient(final RestTemplate restTemplate,
                               final ApplicationParams applicationParams, SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
    }

    @Retryable(value = {HttpServerErrorException.class, SocketTimeoutException.class},
        maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    public Either<ResourceNotFoundException, Document> getDocument(final UUID documentId) {
        try {
            ResponseEntity<Document> response = restTemplate.exchange(
                String.format("%s/documents/%s", applicationParams.getDocumentURL(), documentId),
                HttpMethod.GET,
                getRequestHttpEntity(),
                Document.class
            );

            Document document = response.getBody();
            log.debug("Result of {} metadata call: {}", documentId, document);

            return Either.right(document);
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                return Either.left(new ResourceNotFoundException(
                        String.format(DOCUMENT_METADATA_NOT_FOUND, documentId.toString()),
                        exception));
            }
            throw exception;
        }
    }

    @Retryable(value = {HttpServerErrorException.class, SocketTimeoutException.class},
        maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    @SuppressWarnings("ConstantConditions")
    public ResponseEntity<ByteArrayResource> getDocumentAsBinary(final UUID documentId) {
        try {
            return restTemplate.exchange(
                String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                GET,
                getRequestHttpEntity(),
                ByteArrayResource.class
            );
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                throw new ResourceNotFoundException(String.format("%s %s", RESOURCE_NOT_FOUND, documentId), exception);
            }

            throw exception;
        }
    }

    public void deleteDocument(final UUID documentId, final Boolean permanent) {
        try {
            HttpEntity<Void> entity = getRequestHttpEntity();

            ResponseEntity<Void> response = restTemplate.exchange(
                String.format("%s/documents/%s?permanent=%s", applicationParams.getDocumentURL(), documentId,
                              permanent),
                HttpMethod.DELETE,
                entity,
                Void.class
            );

            log.debug("Delete document {} completed with status: {}", documentId, response.getStatusCode());
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                throw new ResourceNotFoundException(String.format("%s %s", RESOURCE_NOT_FOUND, documentId), exception);
            }
            throw exception;
        }
    }

    public Either<ResourceNotFoundException, PatchDocumentResponse> patchDocument(final UUID documentId,
                                                                         final DmTtlRequest dmTtlRequest) {
        try {
            final PatchDocumentResponse patchDocumentResponse = restTemplate.patchForObject(
                String.format("%s/documents/%s", applicationParams.getDocumentURL(), documentId),
                getRequestHttpEntity(dmTtlRequest),
                PatchDocumentResponse.class
            );

            return Either.right(patchDocumentResponse);
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                return Either.left(new ResourceNotFoundException(
                    String.format("%s %s", RESOURCE_NOT_FOUND, documentId),
                    exception));
            }

            throw exception;
        }
    }

    public void patchDocumentMetadata(final UpdateDocumentsCommand updateDocumentsCommand) {
        restTemplate.patchForObject(
            String.format("%s/documents", applicationParams.getDocumentURL()),
            getRequestHttpEntity(updateDocumentsCommand),
            Void.class
        );
    }

    public DmUploadResponse uploadDocuments(final DocumentUploadRequest documentUploadRequest) {
        return restTemplate.postForObject(
            String.format("%s/documents", applicationParams.getDocumentURL()),
            prepareRequestForUpload(documentUploadRequest),
            DmUploadResponse.class
        );
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> prepareRequestForUpload(final DocumentUploadRequest
                                                                                        documentUploadRequest) {
        LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();

        bodyMap.set(Constants.CLASSIFICATION, documentUploadRequest.getClassification());
        bodyMap.set("metadata[jurisdiction]", documentUploadRequest.getJurisdictionId());
        bodyMap.set("metadata[case_type_id]", documentUploadRequest.getCaseTypeId());
        bodyMap.set("ttl", getEffectiveTTL());

        documentUploadRequest.getFiles()
            .forEach(file -> {
                bodyMap.add(Constants.FILES, file.getResource());
            });

        HttpHeaders headers = prepareRequestHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(bodyMap, headers);
    }

    private String getEffectiveTTL() {
        final ZonedDateTime currentDateTime = ZonedDateTime.now();
        return currentDateTime.plusDays(applicationParams.getDocumentTtlInDays()).format(DM_DATE_TIME_FORMATTER);
    }


    private <T> HttpEntity<T> getRequestHttpEntity() {
        return getRequestHttpEntity(null);
    }

    private <T> HttpEntity<T> getRequestHttpEntity(T body) {
        return new HttpEntity<>(body, prepareRequestHeaders());
    }

    private HttpHeaders prepareRequestHeaders() {
        HttpHeaders headers = securityUtils.serviceAuthorizationHeaders();
        headers.set(Constants.USERID, securityUtils.getUserInfo().getUid());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }
}
