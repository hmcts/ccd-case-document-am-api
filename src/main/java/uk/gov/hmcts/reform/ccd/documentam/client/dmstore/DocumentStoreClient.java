package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.server.ResponseStatusException;
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

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpMethod.GET;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DM_DATE_TIME_FORMATTER;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DOCUMENT_METADATA_NOT_FOUND;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.RESOURCE_NOT_FOUND;

@Component
@Slf4j
public class DocumentStoreClient {

    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;
    public final CloseableHttpClient httpClient;
    private final ApplicationParams applicationParams;

    @Autowired
    public DocumentStoreClient(SecurityUtils securityUtils, final RestTemplate restTemplate,
                               final CloseableHttpClient httpClient,
                               final ApplicationParams applicationParams) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.httpClient = httpClient;
        this.applicationParams = applicationParams;
    }

    @Retryable(value = {HttpServerErrorException.class, SocketTimeoutException.class},
        maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    public Either<ResourceNotFoundException, Document> getDocument(final UUID documentId) {
        try {
            final Document document = restTemplate.getForObject(
                String.format("%s/documents/%s", applicationParams.getDocumentURL(), documentId),
                Document.class
            );

            log.debug("Result of {} metadata call: {}", documentId, document);

            return Either.right(document);
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                return Either.left(new ResourceNotFoundException(
                    String.format(DOCUMENT_METADATA_NOT_FOUND, documentId.toString()),
                    exception
                ));
            }
            throw exception;
        }
    }

    @Retryable(value = {HttpServerErrorException.class, SocketTimeoutException.class},
        maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    @SuppressWarnings("ConstantConditions")
    public ResponseEntity<ByteArrayResource> getDocumentAsBinary(final UUID documentId) {
        final HttpEntity<Object> nullRequestEntity = null;

        try {
            return restTemplate.exchange(
                String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                GET,
                nullRequestEntity,
                ByteArrayResource.class
            );
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                throw new ResourceNotFoundException(String.format("%s %s", RESOURCE_NOT_FOUND, documentId), exception);
            }

            throw exception;
        }
    }

    @Retryable(value = {HttpServerErrorException.class, SocketTimeoutException.class},
        maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    public void streamDocumentAsBinary(final UUID documentId, HttpServletResponse httpResponseOut,
                                       Map<String, String> requestHeaders) {
        try {
            HttpGet httpRequest = prepareHttpRequest(documentId, requestHeaders);
            CloseableHttpResponse httpClientResponse = httpClient.execute(httpRequest);
            HttpStatus statusCode = HttpStatus.valueOf(httpClientResponse.getStatusLine().getStatusCode());

            handleStreamResponse(statusCode, httpClientResponse, httpResponseOut, documentId);
        } catch (Exception exception) {
            log.error("Error occurred", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                              "Error occurred while processing the request", exception
            );
        }
    }

    private HttpGet prepareHttpRequest(UUID documentId, final Map<String, String> requestHeaders) {
        HttpGet httpGet = new HttpGet(
            String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId));

        setRequestHeaders(httpGet, requestHeaders);

        return httpGet;
    }

    private void setRequestHeaders(final HttpGet httpGet, Map<String, String> requestHeaders) {
        Set<String> filteredHeaders = new HashSet<>(applicationParams.getFilteredRequestHeaders());

        // map client request headers
        if (!filteredHeaders.isEmpty()) {
            requestHeaders.forEach((name, value) -> {
                if (filteredHeaders.stream().anyMatch(name::equalsIgnoreCase)) {
                    httpGet.addHeader(name.toLowerCase(), value);
                }
            });
        }

        HttpHeaders headers = securityUtils.serviceAuthorizationHeaders();
        headers.forEach((headerName, headerValues) ->
            headerValues.forEach(headerValue -> httpGet.addHeader(headerName, headerValue))
        );
        httpGet.addHeader(Constants.USERID, securityUtils.getUserInfo().getUid());
        httpGet.addHeader(HttpHeaders.CONTENT_TYPE, String.valueOf(MediaType.APPLICATION_JSON));
    }


    private void handleStreamResponse(HttpStatus statusCode,
                                      CloseableHttpResponse httpClientResponse,
                                      HttpServletResponse httpResponseOut,
                                      UUID documentId) {
        switch (statusCode) {
            case OK -> {
                httpResponseOut.setStatus(statusCode.value());
                mapResponseHeaders(httpClientResponse, httpResponseOut);
                try {
                    try (InputStream input = httpClientResponse.getEntity().getContent()) {
                        OutputStream output = httpResponseOut.getOutputStream();
                        byte[] buffer = new byte[1024 * 1024]; // 1 MB buffer
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) >= 0) {
                            output.write(buffer, 0, bytesRead);
                        }
                    }
                } catch (Throwable t) {
                    log.error("Error transferring document {} : {}", documentId, t.getMessage());
                }

            }
            case NOT_FOUND ->
                throw new ResourceNotFoundException(String.format("%s %s", RESOURCE_NOT_FOUND, documentId), null);
            case INTERNAL_SERVER_ERROR, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT ->
                throw new HttpServerErrorException(statusCode, String.format("Failed to retrieve document with ID: "
                                                                                 + "%s", documentId));
            default ->
                throw new ResponseStatusException(
                    statusCode,
                    String.format("Failed to retrieve document with ID: %s", documentId)
                );
        }
    }

    private void mapResponseHeaders(CloseableHttpResponse httpClientResponse, HttpServletResponse httpResponseOut) {
        for (Header header : httpClientResponse.getAllHeaders()) {
            httpResponseOut.setHeader(header.getName(), header.getValue());
        }
    }

    public void deleteDocument(final UUID documentId, final Boolean permanent) {
        try {
            restTemplate.delete(String.format(
                "%s/documents/%s?permanent=%s",
                applicationParams.getDocumentURL(),
                documentId,
                permanent
            ));
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
                dmTtlRequest,
                PatchDocumentResponse.class
            );

            return Either.right(patchDocumentResponse);
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                return Either.left(new ResourceNotFoundException(
                    String.format("%s %s", RESOURCE_NOT_FOUND, documentId),
                    exception
                ));
            }

            throw exception;
        }
    }

    public void patchDocumentMetadata(final UpdateDocumentsCommand updateDocumentsCommand) {
        restTemplate.patchForObject(
            String.format("%s/documents", applicationParams.getDocumentURL()),
            updateDocumentsCommand,
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(bodyMap, headers);
    }

    private String getEffectiveTTL() {
        final ZonedDateTime currentDateTime = ZonedDateTime.now();
        return currentDateTime.plusDays(applicationParams.getDocumentTtlInDays()).format(DM_DATE_TIME_FORMATTER);
    }
}
