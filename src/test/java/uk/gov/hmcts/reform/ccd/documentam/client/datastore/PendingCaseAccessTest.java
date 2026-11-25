package uk.gov.hmcts.reform.ccd.documentam.client.datastore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PendingCaseAccessTest {
    private static final String CASE_ID = "1234567890123456";
    private static final String URL = "http://ccd/cases/" + CASE_ID;
    private final RestTemplate http = mock(RestTemplate.class);
    private final SecurityUtils security = mock(SecurityUtils.class);
    private final HttpHeaders headers = new HttpHeaders();
    private CaseDataStoreClientImpl client;

    @BeforeEach
    void setUp() {
        headers.setBearerAuth("synthetic-user-token");
        headers.set("ServiceAuthorization", "synthetic-service-token");
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        when(security.authorizationHeaders()).thenReturn(headers);
        client = new CaseDataStoreClientImpl(http, "http://ccd", security);
    }

    private void respond(String body) throws Exception {
        when(http.exchange(eq(URL), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(JsonNode.class)))
            .thenReturn(ResponseEntity.ok(new ObjectMapper().readTree(body)));
    }

    @Test
    void checksUserAuthorizedResponseAndMatchingContext() throws Exception {
        respond("{\"id\":\"1234567890123456\",\"case_type\":\"type\",\"jurisdiction\":\"jurisdiction\"}");
        assertDoesNotThrow(() -> client.checkCaseAccess(CASE_ID, "type", "jurisdiction"));
        verify(http).exchange(URL, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"id\":\"9999999999999999\",\"case_type\":\"type\",\"jurisdiction\":\"jurisdiction\"}",
        "{\"id\":\"1234567890123456\",\"case_type\":\"other\",\"jurisdiction\":\"jurisdiction\"}",
        "{\"id\":\"1234567890123456\",\"case_type\":\"type\",\"jurisdiction\":\"other\"}"
    })
    void rejectsMissingOrSubstitutedCaseContext(String body) throws Exception {
        respond(body);
        assertThrows(ForbiddenException.class, () -> client.checkCaseAccess(CASE_ID, "type", "jurisdiction"));
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404})
    void deniedOrRevokedCaseAccessFailsClosed(int status) {
        when(http.exchange(eq(URL), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(JsonNode.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.valueOf(status)));
        assertThrows(ForbiddenException.class, () -> client.checkCaseAccess(CASE_ID, "type", "jurisdiction"));
    }

    @Test
    void missingCaseBodyFailsClosed() {
        when(http.exchange(eq(URL), eq(HttpMethod.GET), eq(new HttpEntity<>(headers)), eq(JsonNode.class)))
            .thenReturn(ResponseEntity.ok().build());
        assertThrows(ForbiddenException.class, () -> client.checkCaseAccess(CASE_ID, "type", "jurisdiction"));
    }
}
