package uk.gov.hmcts.reform.ccd.document.am.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.Map;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SIZE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CREATED_BY;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.MODIFIED_ON;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.METADATA;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ROLES;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.TTL;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DOCUMENT_LINKS;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CREATED_ON;

public class ResponseHelper {

    private ResponseHelper() {
    }

    public static ResponseEntity<Object> toResponseEntity(ResponseEntity<StoredDocumentHalResource> response, UUID documentId) {
        Optional<?> payload = Optional.of(response.getBody());
        addHateoasLinks(payload,documentId);

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

    public static ResponseEntity<Object>  updatePatchTTLResponse(ResponseEntity<StoredDocumentHalResource> updateResponse) {
        LinkedHashMap<String, Object> updatedUploadedDocumentResponse = new LinkedHashMap<>();
        ObjectMapper m = new ObjectMapper();
        try {
            StoredDocumentHalResource storedDocumentHalResource =  updateResponse.getBody();
            Map<String,Object> metaData = m.convertValue(storedDocumentHalResource, new TypeReference<Map<String, Object>>() {});
            updateResponseFields(storedDocumentHalResource.getTtl(), storedDocumentHalResource.getCreatedOn(), metaData);
            return new ResponseEntity<>(metaData, convertHeaders(updateResponse.getHeaders()), updateResponse.getStatusCode());

        } catch (Exception exception) {
            throw new ResponseFormatException("Error while updating patch TTL response " + exception);
        }
    }

    private static void updateResponseFields(Date ttl, Date createdOn, Map<String, Object> metaData) {
        metaData.remove(SIZE);
        metaData.remove(CREATED_BY);
        metaData.remove(MODIFIED_ON);
        metaData.remove(METADATA);
        metaData.remove(ROLES);
        metaData.remove(DOCUMENT_LINKS);
        metaData.put(TTL, ttl);
        metaData.put(CREATED_ON, createdOn);
    }

    public static void addHateoasLinks(Optional<?> payload,UUID documentId) {
        if (payload.isPresent()) {
            Object obj = payload.get();
            if (obj instanceof StoredDocumentHalResource) {
                ((StoredDocumentHalResource) obj).addLinks(documentId);
            }

        }

    }
}
