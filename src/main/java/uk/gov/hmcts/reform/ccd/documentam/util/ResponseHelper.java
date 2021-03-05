package uk.gov.hmcts.reform.ccd.documentam.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResponseFormatException;
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

    public static ResponseEntity<Object> toResponseEntity(ResponseEntity<StoredDocumentHalResource> response,
                                                          UUID documentId) {
        Optional<?> payload = Optional.of(response.getBody());
        addHateoasLinks(payload, documentId);

        return new ResponseEntity<>(
            payload.orElse(null),
            convertHeaders(response.getHeaders()),
            response.getStatusCode());
    }

    public static MultiValueMap<String, String> convertHeaders(HttpHeaders responseHeaders) {
        MultiValueMap<String, String> responseEntityHeaders = new LinkedMultiValueMap<>();
        responseHeaders.entrySet().stream().forEach(e -> {
            if (!(e.getKey().equalsIgnoreCase("request-context") || e.getKey().equalsIgnoreCase("x-powered-by"))) {
                responseEntityHeaders.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        });

        return responseEntityHeaders;
    }

    public static ResponseEntity<Object> updatePatchTTLResponse(
        ResponseEntity<StoredDocumentHalResource> updateResponse) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            StoredDocumentHalResource storedDocumentHalResource = updateResponse.getBody();
            Map<String, Object> metaData = mapper.convertValue(storedDocumentHalResource,
                new TypeReference<>() {
                });
            updateResponseFields(storedDocumentHalResource.getTtl(), storedDocumentHalResource.getCreatedOn(),
                storedDocumentHalResource.getModifiedOn(), metaData);
            return new ResponseEntity<>(metaData, convertHeaders(updateResponse.getHeaders()),
                updateResponse.getStatusCode());

        } catch (Exception exception) {
            throw new ResponseFormatException("Error while updating patch TTL response " + exception);
        }
    }

    private static void updateResponseFields(Date ttl, Date createdOn, Date modifiedOn, Map<String, Object> metaData) {
        metaData.remove(Constants.SIZE);
        metaData.remove(Constants.CREATED_BY);
        metaData.remove(Constants.CLASSIFICATION);
        metaData.remove(Constants.METADATA);
        metaData.remove(Constants.ROLES);
        metaData.remove(Constants.DOCUMENT_LINKS);
        metaData.put(Constants.TTL, ttl);
        metaData.put(Constants.CREATED_ON, createdOn);
        metaData.put(Constants.MODIFIED_ON, modifiedOn);
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
