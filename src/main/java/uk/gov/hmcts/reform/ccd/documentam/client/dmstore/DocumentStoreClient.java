package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.mime.ContentBody;
import org.apache.hc.client5.http.entity.mime.InputStreamBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
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
import org.springframework.web.multipart.MultipartFile;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
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

    private final RestTemplate restTemplate;
    public final CloseableHttpClient httpClient;
    private final ApplicationParams applicationParams;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecurityUtils securityUtils;


    @Autowired
    public DocumentStoreClient(SecurityUtils securityUtils, final RestTemplate restTemplate,
                               final CloseableHttpClient httpClient,
                               final ApplicationParams applicationParams) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.httpClient = httpClient;
        this.applicationParams = applicationParams;
    }

    @Retryable(retryFor = {HttpServerErrorException.class, SocketTimeoutException.class},
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
                    exception
                ));
            }
            throw exception;
        }
    }

    @Retryable(retryFor = {HttpServerErrorException.class, SocketTimeoutException.class},
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

    @Retryable(retryFor = {HttpServerErrorException.class, SocketTimeoutException.class},
        maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    public void streamDocumentAsBinary(final UUID documentId, HttpServletResponse httpResponseOut,
                                       Map<String, String> requestHeaders) {
        try {
            HttpGet httpRequest = buildStreamBinaryHttpRequest(documentId, requestHeaders);
            try (ClassicHttpResponse httpClientResponse = httpClient.executeOpen(null, httpRequest, null)) {
                HttpStatus statusCode = HttpStatus.valueOf(httpClientResponse.getCode());
                handleDownloadStreamResponse(statusCode, httpClientResponse, httpResponseOut, documentId);
                httpClientResponse.close();
            }
        } catch (IOException exception) {
            log.error("Error occurred", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                              "Error occurred while processing the request: " + exception.getMessage(),
                                              exception
            );
        }
    }

    private HttpGet buildStreamBinaryHttpRequest(UUID documentId, final Map<String, String> requestHeaders) {
        HttpGet httpGet = new HttpGet(
            String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId));

        httpGet.addHeader(HttpHeaders.CONTENT_TYPE, String.valueOf(MediaType.APPLICATION_JSON));

        setForwardedHeaders(requestHeaders, httpGet);
        setCommonRequestHeaders(httpGet);

        return httpGet;
    }

    private void setForwardedHeaders(Map<String, String> requestHeaders, HttpGet httpGet) {
        Set<String> filteredHeaders = new HashSet<>(applicationParams.getClientRequestHeadersToForward());

        if (!filteredHeaders.isEmpty()) {
            requestHeaders.forEach((name, value) -> {
                if (filteredHeaders.stream().anyMatch(name::equalsIgnoreCase)) {
                    httpGet.addHeader(name.toLowerCase(), value);
                }
            });
        }
    }

    private void handleDownloadStreamResponse(HttpStatus statusCode,
                                              ClassicHttpResponse httpClientResponse,
                                              HttpServletResponse httpResponseOut,
                                              UUID documentId) throws IOException {
        switch (statusCode) {
            case OK, PARTIAL_CONTENT -> {
                httpResponseOut.setStatus(statusCode.value());
                mapResponseHeaders(httpClientResponse.getHeaders(), httpResponseOut);

                try (InputStream input = httpClientResponse.getEntity().getContent()) {
                    OutputStream output = httpResponseOut.getOutputStream();
                    byte[] buffer = new byte[1024 * 1024]; // 1 MB buffer
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) >= 0) {
                        output.write(buffer, 0, bytesRead);
                    }
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

    private void setCommonRequestHeaders(final HttpUriRequestBase httpBase) {
        HttpHeaders headers = securityUtils.serviceAuthorizationHeaders();
        headers.forEach((headerName, headerValues) ->
                            headerValues.forEach(headerValue -> httpBase.addHeader(headerName, headerValue))
        );
        httpBase.addHeader(Constants.USERID, securityUtils.getUserInfo().getUid());
    }

    private void mapResponseHeaders(Header[] responseHeaders, HttpServletResponse httpResponseOut) {
        for (Header header : responseHeaders) {
            httpResponseOut.setHeader(header.getName(), header.getValue());
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
                    exception
                ));
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
            buildUploadEntity(documentUploadRequest),
            DmUploadResponse.class
        );
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> buildUploadEntity(final DocumentUploadRequest
                                                                                        documentUploadRequest) {
        LinkedMultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();

        bodyMap.set(Constants.CLASSIFICATION, documentUploadRequest.getClassification());
        bodyMap.set("metadata[jurisdiction]", documentUploadRequest.getJurisdictionId());
        bodyMap.set("metadata[case_type_id]", documentUploadRequest.getCaseTypeId());
        bodyMap.set("ttl", getEffectiveTTL());

        documentUploadRequest.getFiles()
            .forEach(file -> bodyMap.add(Constants.FILES, file.getResource()));

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

    @Retryable(retryFor = {HttpServerErrorException.class, SocketTimeoutException.class},
        maxAttemptsExpression = "${retry.maxAttempts}",
        backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    public DmUploadResponse uploadDocumentsAsStream(final DocumentUploadRequest documentUploadRequest) {
        try {
            HttpPost request = buildStreamUploadHttpRequest(documentUploadRequest);
            try (ClassicHttpResponse httpClientResponse = httpClient.executeOpen(null, request, null)) {
                HttpStatus statusCode = HttpStatus.valueOf(httpClientResponse.getCode());
                DmUploadResponse uploadResponse = handleUploadStreamResponse(statusCode, httpClientResponse);
                httpClientResponse.close();
                return uploadResponse;
            }
        } catch (IOException exception) {
            log.error("Error occurred", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                              "Error occurred while processing the request: " + exception.getMessage(),
                                              exception
            );
        }
    }

    private DmUploadResponse handleUploadStreamResponse(HttpStatus statusCode,
                                                        ClassicHttpResponse httpClientResponse) throws IOException {
        var responseEntity = httpClientResponse.getEntity();
        String responseBody = null;

        if (responseEntity != null) {
            try (InputStream responseStream = responseEntity.getContent()) {
                responseBody = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        if (statusCode.is2xxSuccessful()) {
            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    return objectMapper.readValue(responseBody, DmUploadResponse.class);
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse success response: {}", responseBody, e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                      "Failed to parse server response", e);
                }
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Empty response from server");
        }

        if (statusCode == HttpStatus.UNPROCESSABLE_ENTITY) {
            String errorFallback = HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase();

            throw HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY,
                errorFallback,
               null,
                responseBody != null ? responseBody.getBytes(StandardCharsets.UTF_8)
                    : errorFallback.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
            );

        }

        if (statusCode.is5xxServerError()) {
            throw new HttpServerErrorException(statusCode,
                                               "Document upload failed due to server error. Response: " + responseBody);
        }

        throw new ResponseStatusException(statusCode, "Document upload failed with status: " + statusCode + ". "
            + "Response: " + responseBody);
    }

    private HttpPost buildStreamUploadHttpRequest(final DocumentUploadRequest documentUploadRequest)
        throws IOException {
        HttpPost httpPost = new HttpPost(
            String.format("%s/documents", applicationParams.getDocumentURL()));
        setCommonRequestHeaders(httpPost);
        httpPost.setEntity(buildUploadStreamEntity(documentUploadRequest));

        return httpPost;
    }

    public org.apache.hc.core5.http.HttpEntity buildUploadStreamEntity(DocumentUploadRequest documentUploadRequest)
        throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addTextBody(Constants.CLASSIFICATION, documentUploadRequest.getClassification());
        builder.addTextBody("metadata[jurisdiction]", documentUploadRequest.getJurisdictionId());
        builder.addTextBody("metadata[case_type_id]", documentUploadRequest.getCaseTypeId());
        builder.addTextBody("ttl", getEffectiveTTL());

        for (MultipartFile fileInfo : documentUploadRequest.getFiles()) {
            InputStream inputStream = fileInfo.getInputStream();
            String filename = fileInfo.getOriginalFilename();
            String type = fileInfo.getContentType();
            ContentType contentType = type != null ? ContentType.parse(type) : ContentType.DEFAULT_BINARY;

            // Note: The InputStream provided to InputStreamBody is closed within the
            // org.apache.http.entity.mime.content.InputStreamBody#writeTo(OutputStream) method
            // after all data has been fully written to the output stream.
            ContentBody contentBody = new InputStreamBody(inputStream, contentType, filename);
            builder.addPart(Constants.FILES, contentBody);
        }
        return builder.build();
    }
}
