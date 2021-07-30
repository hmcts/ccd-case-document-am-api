package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpMethod.GET;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DM_DATE_TIME_FORMATTER;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.RESOURCE_NOT_FOUND;

@Component
public class DocumentStoreClient {

    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;

    @Autowired
    public DocumentStoreClient(final RestTemplate restTemplate,
                               final ApplicationParams applicationParams) {
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
    }

    public Optional<Document> getDocument(final UUID documentId) {
        try {
            final Document document = restTemplate.getForObject(
                String.format("%s/documents/%s", applicationParams.getDocumentURL(), documentId),
                Document.class
            );

            return Optional.ofNullable(document);
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                return Optional.empty();
            }
            throw new ServiceException(
                String.format(EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE, documentId),
                exception
            );
        }
    }

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
                throw new ResourceNotFoundException(
                    String.format("%s %s", RESOURCE_NOT_FOUND, documentId),
                    exception
                );
            }

            throw new ServiceException(
                String.format(EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE, documentId),
                exception
            );
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
            handleException(exception, documentId.toString());
        }
    }

    public Optional<PatchDocumentResponse> patchDocument(final UUID documentId, final DmTtlRequest dmTtlRequest) {
        try {
            final PatchDocumentResponse patchDocumentResponse = restTemplate.patchForObject(
                String.format("%s/documents/%s", applicationParams.getDocumentURL(), documentId),
                dmTtlRequest,
                PatchDocumentResponse.class
            );

            return Optional.ofNullable(patchDocumentResponse);
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                return Optional.empty();
            }
            throw new ServiceException(String.format(EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE, documentId), exception);
        }
    }

    public void patchDocumentMetadata(final UpdateDocumentsCommand updateDocumentsCommand) {
        try {
            restTemplate.patchForObject(
                String.format("%s/documents", applicationParams.getDocumentURL()),
                updateDocumentsCommand,
                Void.class
            );
        } catch (HttpClientErrorException exception) {
            throw new ServiceException(Constants.EXCEPTION_ERROR_MESSAGE, exception);
        }
    }

    public DmUploadResponse uploadDocuments(final DocumentUploadRequest documentUploadRequest) {
        try {
            return restTemplate.postForObject(
                String.format("%s/documents", applicationParams.getDocumentURL()),
                prepareRequestForUpload(documentUploadRequest),
                DmUploadResponse.class
            );
        } catch (HttpClientErrorException exception) {
            throw new ServiceException(Constants.EXCEPTION_ERROR_MESSAGE, exception);
        }
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
                if (!file.isEmpty()) {
                    bodyMap.add(Constants.FILES, file.getResource());
                }
            });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(bodyMap, headers);
    }

    private String getEffectiveTTL() {
        final ZonedDateTime currentDateTime = ZonedDateTime.now();
        return currentDateTime.plusDays(applicationParams.getDocumentTtlInDays()).format(DM_DATE_TIME_FORMATTER);
    }

    private void handleException(final HttpClientErrorException exception, final String parameter) {
        if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
            throw new ResourceNotFoundException(String.format("%s %s", RESOURCE_NOT_FOUND, parameter), exception);
        }

        throw new ServiceException(String.format(EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE, parameter), exception);
    }

}
