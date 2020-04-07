package uk.gov.hmcts.reform.ccd.document.am.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResponseHelperTest {

    @Test
    void convertHeaders() {
        MultiValueMap<String, String> result = ResponseHelper.convertHeaders(getHttpHeaders());
        assertEquals(2, result.size());
        assertNotNull(result.getFirst("userId"));
        assertNotNull(result.getFirst("Content-Type"));
    }

    @Test
    void addHateoasLinks() {
        StoredDocumentHalResource name = new StoredDocumentHalResource();
        Optional<StoredDocumentHalResource> opt = Optional.of(name);
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
