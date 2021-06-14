package uk.gov.hmcts.reform.ccd.documentam.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class ResponseHelperTest {

    @Test
    void shouldGetToResponseEntity() {
        StoredDocumentHalResource resource = new StoredDocumentHalResource();
        ResponseEntity<StoredDocumentHalResource> responseEntity = new ResponseEntity<>(resource, getHttpHeaders(),
                                                                                        HttpStatus.OK);
        ResponseEntity<StoredDocumentHalResource> result = ResponseHelper.toResponseEntity(
            responseEntity,
            UUID.fromString("f565abb5-c337-4ccb-ba78-1c43989e3bd6")
        );
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getHeaders().size());
        assertNotNull(result.getBody());
    }

    @Test
    void convertHeaders() {
        MultiValueMap<String, String> result = ResponseHelper.convertHeaders(getHttpHeaders());
        assertEquals(2, result.size());
        assertNotNull(result.getFirst("userId"));
        assertNotNull(result.getFirst("Content-Type"));
    }

    @Test
    void addHateoasLinksForOtherObjects() {
        Optional<Object> opt = Optional.of(new Object());
        ResponseHelper.addHateoasLinks(opt, UUID.fromString("f565abb5-c337-4ccb-ba78-1c43989e3bd5"));
        assertNotNull(opt);
    }

    @Test
    void addHateoasLinksForEmpltyObjects() {
        Optional<Object> opt = Optional.empty();
        ResponseHelper.addHateoasLinks(opt, UUID.fromString("f565abb5-c337-4ccb-ba78-1c43989e3bd5"));
        assertNotNull(opt);
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("userId", "userId");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("request-context", "context");
        headers.set("x-powered-by", "powered-by");
        return headers;
    }
}
