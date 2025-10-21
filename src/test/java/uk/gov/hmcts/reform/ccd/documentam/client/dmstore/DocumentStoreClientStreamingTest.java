package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentStoreClientStreamingTest {

    private static final String UID = "123456789";

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private PoolingHttpClientConnectionManager httpClientConnectionManager;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private Header header1;

    @Mock
    private Header header2;

    @Mock
    private ApplicationParams applicationParams;

    @Captor
    private ArgumentCaptor<HttpGet> httpGetCaptor;

    private DocumentStoreClient documentStoreClient;

    private AutoCloseable openMocks;

    MockHttpServletResponse httpResponseOut;

    UUID documentId;

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);

        documentId = UUID.randomUUID();
        httpResponseOut = new MockHttpServletResponse();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "test");
        when(securityUtils.serviceAuthorizationHeaders()).thenReturn(headers);
        when(securityUtils.getUserInfo()).thenReturn(UserInfo.builder().uid(UID).build());

        when(applicationParams.getClientRequestHeadersToForward()).thenReturn(List.of("RANGE"));

        documentStoreClient = new DocumentStoreClient(securityUtils, restTemplate, httpClient,
                                                      httpClientConnectionManager, applicationParams);
    }

    @Test
    void testStreamDocumentAsBinary_Success() throws IOException {
        final Map<String, String> requestHeaders = Map.of("Authorization", "Bearer token");
        when(httpResponse.getCode()).thenReturn(200);
        when(httpResponse.getReasonPhrase()).thenReturn("OK");
        HttpEntity entity = new BasicHttpEntity(new ByteArrayInputStream("Document content".getBytes()), null);
        when(httpResponse.getEntity()).thenReturn(entity);

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");
        when(header2.getName()).thenReturn("X-Custom-Header");
        when(header2.getValue()).thenReturn("custom-value");
        when(httpResponse.getHeaders()).thenReturn(new Header[]{header1, header2});

        when(httpClient.executeOpen(any(), any(HttpGet.class), any())).thenReturn(httpResponse);

        documentStoreClient.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders);

        assertEquals(HttpServletResponse.SC_OK, httpResponseOut.getStatus());
        assertEquals("application/json", httpResponseOut.getHeader(HttpHeaders.CONTENT_TYPE));
        assertEquals("custom-value", httpResponseOut.getHeader("X-Custom-Header"));

        verify(httpClient).executeOpen(any(), httpGetCaptor.capture(), any());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getPath()
        );

        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @Test
    void testStreamDocumentAsBinary_PartialContent() throws IOException {
        final Map<String, String> requestHeaders =
            Map.of("Authorization", "Bearer token","Range", "bytes=0-999");

        when(httpResponse.getCode()).thenReturn(206);
        when(httpResponse.getReasonPhrase()).thenReturn("Partial Content");

        HttpEntity entity = new BasicHttpEntity(
            new ByteArrayInputStream("Partial document content".getBytes()),
            null
        );
        when(httpResponse.getEntity()).thenReturn(entity);

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");
        when(header2.getName()).thenReturn("Access-Control-Allow-Headers");
        when(header2.getValue()).thenReturn("Accept-Ranges");
        when(httpResponse.getHeaders()).thenReturn(new Header[]{header1, header2});

        when(httpClient.executeOpen(any(), any(HttpGet.class), any())).thenReturn(httpResponse);

        documentStoreClient.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders);

        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, httpResponseOut.getStatus());
        assertEquals("application/json", httpResponseOut.getHeader(HttpHeaders.CONTENT_TYPE));
        assertEquals("Accept-Ranges", httpResponseOut.getHeader("Access-Control-Allow-Headers"));

        verify(httpClient).executeOpen(any(), httpGetCaptor.capture(), any());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getPath());

        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @Test
    void testStreamDocumentAsBinary_NotFound() throws IOException {
        final Map<String, String> requestHeaders = Collections.singletonMap("Authorization", "Bearer token");

        when(httpResponse.getCode()).thenReturn(404);
        when(httpResponse.getReasonPhrase()).thenReturn("Internal server error");
        when(httpClient.executeOpen(any(), any(HttpGet.class), any())).thenReturn(httpResponse);

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");

        assertThrows(ResourceNotFoundException.class,
                     () -> documentStoreClient.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        verify(httpClient).executeOpen(any(), httpGetCaptor.capture(), any());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getPath()
        );
        verify(securityUtils).serviceAuthorizationHeaders();
    }

    @Test
    void testStreamDocumentAsBinary_ServerError() throws IOException {
        final Map<String, String> requestHeaders = Collections.singletonMap("Authorization", "Bearer token");

        when(httpResponse.getCode()).thenReturn(503);
        when(httpResponse.getReasonPhrase()).thenReturn("Service Unavailable");

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");
        when(httpResponse.getHeaders()).thenReturn(new Header[]{header1});

        when(httpClient.executeOpen(any(), any(HttpGet.class), any())).thenReturn(httpResponse);

        assertThrows(HttpServerErrorException.class,
                     () -> documentStoreClient.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        verify(httpClient).executeOpen(any(), httpGetCaptor.capture(), any());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getPath()
        );

        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @Test
    void testStreamDocumentAsBinary_DefaultError() throws IOException {
        final Map<String, String> requestHeaders = Map.of("Authorization", "Bearer token");

        when(httpResponse.getCode()).thenReturn(403);
        when(httpResponse.getReasonPhrase()).thenReturn("Service Unavailable");

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");
        when(httpResponse.getHeaders()).thenReturn(new Header[]{header1});

        when(httpClient.executeOpen(any(), any(HttpGet.class), any())).thenReturn(httpResponse);

        assertThrows(ResponseStatusException.class,
                     () -> documentStoreClient.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        verify(httpClient).executeOpen(any(), httpGetCaptor.capture(), any());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getPath()
        );

        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @Test
    void testStreamDocumentAsBinary_IOError() throws IOException {
        final Map<String, String> requestHeaders = Map.of("Authorization", "Bearer token");

        when(httpResponse.getCode()).thenReturn(200);
        when(httpResponse.getReasonPhrase()).thenReturn("OK");

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");
        when(header2.getName()).thenReturn("X-Custom-Header");
        when(header2.getValue()).thenReturn("custom-value");
        when(httpResponse.getHeaders()).thenReturn(new Header[]{header1, header2});

        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);

        InputStream mockInputStream = mock(InputStream.class);
        doThrow(new IOException("Mocked IOException")).when(mockInputStream).read(any(byte[].class));

        when(httpClient.executeOpen(any(), any(HttpGet.class), any())).thenReturn(httpResponse);
        when(httpResponse.getEntity().getContent()).thenReturn(mockInputStream);

        ResponseStatusException thrown = assertThrows(
            ResponseStatusException.class,
            () -> documentStoreClient.streamDocumentAsBinary(documentId,
                                                             httpResponseOut,
                                                             requestHeaders
            )
        );
        assertNotNull(thrown.getCause());
        assertInstanceOf(IOException.class, thrown.getCause());
        assertEquals("Mocked IOException", thrown.getCause().getMessage());

        verify(httpClient).executeOpen(any(), httpGetCaptor.capture(), any());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getPath()
        );

        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }
}
