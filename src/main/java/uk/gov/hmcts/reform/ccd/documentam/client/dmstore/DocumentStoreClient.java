package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import io.vavr.control.Either;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.springframework.http.HttpMethod.GET;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DM_DATE_TIME_FORMATTER;

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

    public Either<HttpClientErrorException, Document> getDocument(final UUID documentId) {
        try {
            final Document document = restTemplate.getForObject(
                String.format("%s/documents/%s", applicationParams.getDocumentURL(), documentId),
                Document.class
            );

            return Either.right(document);
        } catch (HttpClientErrorException exception) {
            return Either.left(exception);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public ResponseEntity<ByteArrayResource> getDocumentAsBinary(final UUID documentId) {
        final HttpEntity<Object> nullRequestEntity = null;

        return restTemplate.exchange(
            String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
            GET,
            nullRequestEntity,
            ByteArrayResource.class
        );
    }

    public void deleteDocument(final UUID documentId, final Boolean permanent) {
        restTemplate.delete(String.format(
            "%s/documents/%s?permanent=%s",
            applicationParams.getDocumentURL(),
            documentId,
            permanent
        ));
    }

    public Either<HttpClientErrorException, PatchDocumentResponse> patchDocument(final UUID documentId,
                                                                                 final DmTtlRequest dmTtlRequest) {
        try {
            final PatchDocumentResponse patchDocumentResponse = restTemplate.patchForObject(
                String.format("%s/documents/%s", applicationParams.getDocumentURL(), documentId),
                dmTtlRequest,
                PatchDocumentResponse.class
            );

            return Either.right(patchDocumentResponse);
        } catch (HttpClientErrorException exception) {
            return Either.left(exception);
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

}
