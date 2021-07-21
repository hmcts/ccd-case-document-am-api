package uk.gov.hmcts.reform.ccd.documentam.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ResponseHelper {

    private ResponseHelper() {
    }

    public static ResponseEntity<StoredDocumentHalResource> toResponseEntity(
                                                        ResponseEntity<StoredDocumentHalResource> response,
                                                        UUID documentId) {
        Optional<StoredDocumentHalResource> payload = Optional.of(response.getBody());
        addHateoasLinks(payload, documentId);

        return new ResponseEntity<>(
            payload.orElse(null),
            convertHeaders(response.getHeaders()),
            response.getStatusCode());
    }

    public static MultiValueMap<String, String> convertHeaders(HttpHeaders responseHeaders) {
        MultiValueMap<String, String> responseEntityHeaders = new LinkedMultiValueMap<>();
        responseHeaders.entrySet().stream()
            .forEach(e -> {
                if (!(e.getKey().equalsIgnoreCase("request-context") || e.getKey().equalsIgnoreCase("x-powered-by"))) {
                    responseEntityHeaders.put(e.getKey(), new ArrayList<>(e.getValue()));
                }
            });

        return responseEntityHeaders;
    }

    private static PatchDocumentResponse updateResponseFields(Date ttl,
                                                              Date createdOn,
                                                              //Date modifiedOn,
                                                              Map<String, Object> metaData) {
        return PatchDocumentResponse.builder()
            .ttl(ttl)
            .createdOn(createdOn)
            //.modifiedOn(modifiedOn)
            .originalDocumentName(String.valueOf(metaData.get(Constants.ORIGINAL_DOCUMENT_NAME)))
            .mimeType(String.valueOf(metaData.get(Constants.MIME_TYPE)))
            .lastModifiedBy(String.valueOf(metaData.get(Constants.LAST_MODIFIED_BY)))
            .build();
    }

    public static void addHateoasLinks(Optional<?> payload, UUID documentId) {
        if (payload.isPresent()) {
            Object obj = payload.get();
            if (obj instanceof StoredDocumentHalResource) {
                ((StoredDocumentHalResource) obj).addLinks(documentId);
            }

        }

    }
}
