package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableRetry
public class DocumentStoreClientIT extends BaseTest implements TestFixture {

    @MockBean(name = "restTemplate")
    private RestTemplate restTemplate;

    @Autowired
    DocumentStoreClient documentStoreClient;

    @Autowired
    ApplicationParams applicationParams;


    @Test
    void testShouldRetryGetDocumentAfterHttpServerErrorException() {
        when(restTemplate.getForObject(anyString(), ArgumentMatchers.<Class<Document>>any()))
                 .thenThrow(HttpServerErrorException.class);

        assertThrows(HttpServerErrorException.class, () -> documentStoreClient.getDocument(DOCUMENT_ID));

        verify(restTemplate, times(3)).getForObject(
            String.format("%s/documents/%s", applicationParams.getDocumentURL(), DOCUMENT_ID),
            Document.class
        );
    }

    @Test
    void testShouldNotRetryGetDocumentAfterHttpClientErrorException() {
        when(restTemplate.getForObject(anyString(), ArgumentMatchers.<Class<Document>>any()))
            .thenThrow(HttpClientErrorException.class);

        assertThrows(HttpClientErrorException.class, () -> documentStoreClient.getDocument(DOCUMENT_ID));

        verify(restTemplate, times(1)).getForObject(
            String.format("%s/documents/%s", applicationParams.getDocumentURL(), DOCUMENT_ID),
            Document.class
        );
    }

    @Test
    void testShouldRetryGetDocumentAsBinaryAfterHttpServerErrorException() {
        when(restTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<Document>>any()))
            .thenThrow(HttpServerErrorException.class);

        assertThrows(HttpServerErrorException.class, () -> documentStoreClient.getDocumentAsBinary(DOCUMENT_ID));

        verify(restTemplate, times(3)).exchange(
            String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), DOCUMENT_ID),
            HttpMethod.GET,
            null,
            ByteArrayResource.class
        );
    }

    @Test
    void testShouldNotRetryGetDocumentAsBinaryAfterHttpClientErrorException() {
        when(restTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<Document>>any()))
            .thenThrow(HttpClientErrorException.class);

        assertThrows(HttpClientErrorException.class, () -> documentStoreClient.getDocumentAsBinary(DOCUMENT_ID));

        verify(restTemplate, times(1)).exchange(
            String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), DOCUMENT_ID),
            HttpMethod.GET,
            null,
            ByteArrayResource.class
        );
    }
}
