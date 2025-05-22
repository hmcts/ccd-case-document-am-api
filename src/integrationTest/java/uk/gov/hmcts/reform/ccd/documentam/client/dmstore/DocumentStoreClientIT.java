package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private SecurityUtils securityUtils;

    @Autowired
    private DocumentStoreClient documentStoreClient;

    @Autowired
    private ApplicationParams applicationParams;

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
}
