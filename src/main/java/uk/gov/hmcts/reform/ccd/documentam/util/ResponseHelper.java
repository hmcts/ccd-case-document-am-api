package uk.gov.hmcts.reform.ccd.documentam.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;

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

    public static ResponseEntity<PatchDocumentResponse> updatePatchTTLResponse(
        ResponseEntity<StoredDocumentHalResource> updateResponse) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            StoredDocumentHalResource storedDocumentHalResource = updateResponse.getBody();
            Map<String, Object> metaData = mapper.convertValue(storedDocumentHalResource, new TypeReference<>(){});
            PatchDocumentResponse updatedMetaData = updateResponseFields(storedDocumentHalResource.getTtl(),
                                                                         storedDocumentHalResource.getCreatedOn(),
                                                                         storedDocumentHalResource.getModifiedOn(),
                                                                         metaData);

            return new ResponseEntity<>(updatedMetaData, convertHeaders(updateResponse.getHeaders()),
                updateResponse.getStatusCode());

        } catch (Exception exception) {
            throw new ResponseFormatException("Error while updating patch TTL response " + exception);
        }
    }

    private static PatchDocumentResponse updateResponseFields(Date ttl, Date createdOn,
                                                              Date modifiedOn, Map<String, Object> metaData) {
        return new PatchDocumentResponse(
            ttl,
            createdOn,
            modifiedOn,
            String.valueOf(metaData.get(Constants.ORIGINAL_DOCUMENT_NAME)),
            String.valueOf(metaData.get(Constants.MIME_TYPE)),
            String.valueOf(metaData.get(Constants.LAST_MODIFIED_BY))
        );
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
