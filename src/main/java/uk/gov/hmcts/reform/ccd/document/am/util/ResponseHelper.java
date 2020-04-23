package uk.gov.hmcts.reform.ccd.document.am.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unchecked")
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

    public static void addHateoasLinks(Optional<?> payload,UUID documentId) {
        if (payload.isPresent()) {
            Object obj = payload.get();
            if (obj instanceof StoredDocumentHalResource) {
                ((StoredDocumentHalResource) obj).addLinks(documentId);
            }

        }

    }
}
