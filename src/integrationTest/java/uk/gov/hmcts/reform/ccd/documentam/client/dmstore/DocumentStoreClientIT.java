package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableRetry
public class DocumentStoreClientIT extends BaseTest implements TestFixture {

    @MockitoBean(name = "restTemplate")
    private RestTemplate restTemplate;

    @MockitoBean
    private CloseableHttpClient httpClient;

    @MockitoBean
    private SecurityUtils securityUtils;

    @Autowired
    private DocumentStoreClient documentStoreClient;

    @Autowired
    private ApplicationParams applicationParams;

    private HttpServletResponse httpResponseOut;

    @BeforeEach
    void prepare() {
        final HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.add(Constants.SERVICE_AUTHORIZATION, "service_token");

        when(securityUtils.serviceAuthorizationHeaders()).thenReturn(authHeaders);
        when(securityUtils.getUserInfo()).thenReturn(UserInfo.builder().uid(USER_ID).build());
    }

    @Test
    void testShouldRetryGetDocumentAfterHttpServerErrorException() {
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            ArgumentMatchers.<HttpEntity<Void>>any(),
            ArgumentMatchers.<Class<Document>>any()
        )).thenThrow(HttpServerErrorException.class);

        assertThrows(HttpServerErrorException.class, () -> documentStoreClient.getDocument(DOCUMENT_ID));

        verify(restTemplate, times(3)).exchange(
            eq(String.format("%s/documents/%s", applicationParams.getDocumentURL(), DOCUMENT_ID)),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Document.class)
        );
    }

    @Test
    void testShouldNotRetryGetDocumentAfterHttpClientErrorException() {
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            ArgumentMatchers.<HttpEntity<Void>>any(),
            ArgumentMatchers.<Class<Document>>any()
        )).thenThrow(HttpClientErrorException.class);

        assertThrows(HttpClientErrorException.class, () -> documentStoreClient.getDocument(DOCUMENT_ID));

        verify(restTemplate, times(1)).exchange(
            eq(String.format("%s/documents/%s", applicationParams.getDocumentURL(), DOCUMENT_ID)),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Document.class)
        );
    }

    @Test
    void testShouldRetryGetDocumentAsBinaryAfterHttpServerErrorException() {
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            ArgumentMatchers.<HttpEntity<Void>>any(),
            ArgumentMatchers.<Class<ByteArrayResource>>any()
        )).thenThrow(HttpServerErrorException.class);

        assertThrows(HttpServerErrorException.class, () -> documentStoreClient.getDocumentAsBinary(DOCUMENT_ID));

        verify(restTemplate, times(3)).exchange(
            eq(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), DOCUMENT_ID)),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ByteArrayResource.class)
        );
    }

    @Test
    void testShouldNotRetryGetDocumentAsBinaryAfterHttpClientErrorException() {
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            ArgumentMatchers.<HttpEntity<Void>>any(),
            ArgumentMatchers.<Class<ByteArrayResource>>any()
        )).thenThrow(HttpClientErrorException.class);

        assertThrows(HttpClientErrorException.class, () -> documentStoreClient.getDocumentAsBinary(DOCUMENT_ID));

        verify(restTemplate, times(1)).exchange(
            eq(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), DOCUMENT_ID)),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ByteArrayResource.class)
        );
    }

    @Test
    void testStreamDocumentAsBinaryThrowsExceptionOnError() throws IOException {
        when(httpClient.executeOpen(eq(null), any(HttpGet.class), eq(null))).thenThrow(IOException.class);

        final Map<String, String> requestHeaders = new HashMap<>();
        try {
            documentStoreClient.streamDocumentAsBinary(DOCUMENT_ID, httpResponseOut, requestHeaders);
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            assertTrue(exception.getReason().contains("Error occurred while processing the request"));
        }
    }

    @Test
    void testUploadDocumentsAsStreamThrowsExceptionOnError() throws IOException {
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenThrow(IOException.class);

        final DocumentUploadRequest uploadRequest = new DocumentUploadRequest(
            List.of(new MockMultipartFile("afile", "some".getBytes())),
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
            JURISDICTION_ID_VALUE
        );
        try {
            documentStoreClient.uploadDocumentsAsStream(uploadRequest);
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            assertTrue(exception.getReason().contains("Error occurred while processing the request"));
        }
    }
}
