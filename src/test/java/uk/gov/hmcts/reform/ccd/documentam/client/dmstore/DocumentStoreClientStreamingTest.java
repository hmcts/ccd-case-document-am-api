package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
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

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        documentStoreClient = new DocumentStoreClient(securityUtils, restTemplate, httpClient, applicationParams);
    }

    @Test
    void testStreamDocumentAsBinary_Success() throws IOException {
        final Map<String, String> requestHeaders = Map.of("Authorization", "Bearer token");

        StatusLine statusLine = new BasicStatusLine(new HttpGet().getProtocolVersion(), 200, "OK");
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream("Document content".getBytes()));
        when(httpResponse.getEntity()).thenReturn(entity);

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");
        when(header2.getName()).thenReturn("X-Custom-Header");
        when(header2.getValue()).thenReturn("custom-value");
        when(httpResponse.getAllHeaders()).thenReturn(new Header[]{header1, header2});

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        documentStoreClient.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders);

        assertEquals(HttpServletResponse.SC_OK, httpResponseOut.getStatus());
        assertEquals("application/json", httpResponseOut.getHeader(HttpHeaders.CONTENT_TYPE));
        assertEquals("custom-value", httpResponseOut.getHeader("X-Custom-Header"));

        verify(httpClient).execute(httpGetCaptor.capture());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getURI().toString()
        );

        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @Test
    void testStreamDocumentAsBinary_PartialContent() throws IOException {
        final Map<String, String> requestHeaders =
            Map.of("Authorization", "Bearer token","Range", "bytes=0-999");

        StatusLine statusLine =
            new BasicStatusLine(new HttpGet().getProtocolVersion(), 206,"Partial Content");
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        InputStreamEntity entity =
            new InputStreamEntity(new ByteArrayInputStream("Partial document content".getBytes()));
        when(httpResponse.getEntity()).thenReturn(entity);

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");
        when(header2.getName()).thenReturn("Access-Control-Allow-Headers");
        when(header2.getValue()).thenReturn("Accept-Ranges");
        when(httpResponse.getAllHeaders()).thenReturn(new Header[]{header1, header2});

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        documentStoreClient.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders);

        assertEquals(HttpServletResponse.SC_PARTIAL_CONTENT, httpResponseOut.getStatus());
        assertEquals("application/json", httpResponseOut.getHeader(HttpHeaders.CONTENT_TYPE));
        assertEquals("Accept-Ranges", httpResponseOut.getHeader("Access-Control-Allow-Headers"));

        verify(httpClient).execute(httpGetCaptor.capture());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getURI().toString()
        );

        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @Test
    void testStreamDocumentAsBinary_NotFound() throws IOException {
        final Map<String, String> requestHeaders = Collections.singletonMap("Authorization", "Bearer token");

        StatusLine statusLine = new BasicStatusLine(new HttpGet().getProtocolVersion(), 404,
                                                    "Internal server error");
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");

        assertThrows(ResourceNotFoundException.class,
                     () -> documentStoreClient.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        verify(httpClient).execute(httpGetCaptor.capture());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getURI().toString()
        );
        verify(securityUtils).serviceAuthorizationHeaders();
    }

    @Test
    void testStreamDocumentAsBinary_ServerError() throws IOException {
        final Map<String, String> requestHeaders = Collections.singletonMap("Authorization", "Bearer token");

        StatusLine statusLine = new BasicStatusLine(new HttpGet().getProtocolVersion(), 503,
                                                    "Service Unavailable");
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");
        when(httpResponse.getAllHeaders()).thenReturn(new Header[]{header1});

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        assertThrows(HttpServerErrorException.class,
                     () -> documentStoreClient.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        verify(httpClient).execute(httpGetCaptor.capture());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getURI().toString()
        );

        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @Test
    void testStreamDocumentAsBinary_DefaultError() throws IOException {
        final Map<String, String> requestHeaders = Map.of("Authorization", "Bearer token");

        StatusLine statusLine = new BasicStatusLine(new HttpGet().getProtocolVersion(), 403,
                                                    "Service Unavailable");
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");
        when(httpResponse.getAllHeaders()).thenReturn(new Header[]{header1});

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        assertThrows(ResponseStatusException.class,
                     () -> documentStoreClient.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        verify(httpClient).execute(httpGetCaptor.capture());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getURI().toString()
        );

        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @Test
    void testStreamDocumentAsBinary_IOError() throws IOException {
        final Map<String, String> requestHeaders = Map.of("Authorization", "Bearer token");

        StatusLine statusLine = new BasicStatusLine(new HttpGet().getProtocolVersion(), 200,
                                                    "OK");
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        when(header1.getName()).thenReturn("Content-Type");
        when(header1.getValue()).thenReturn("application/json");
        when(header2.getName()).thenReturn("X-Custom-Header");
        when(header2.getValue()).thenReturn("custom-value");
        when(httpResponse.getAllHeaders()).thenReturn(new Header[]{header1, header2});

        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);

        InputStream mockInputStream = mock(InputStream.class);
        doThrow(new IOException("Mocked IOException")).when(mockInputStream).read(any(byte[].class));

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getEntity().getContent()).thenReturn(mockInputStream);

        ResponseStatusException thrown = assertThrows(
            ResponseStatusException.class,
            () -> documentStoreClient.streamDocumentAsBinary(documentId,
                                                             httpResponseOut,
                                                             requestHeaders
            )
        );
        assertNotNull(thrown.getCause());
        assertTrue(thrown.getCause() instanceof IOException);
        assertEquals("Mocked IOException", thrown.getCause().getMessage());

        verify(httpClient).execute(httpGetCaptor.capture());
        HttpGet capturedHttpGet = httpGetCaptor.getValue();
        assertNotNull(capturedHttpGet);
        assertEquals(String.format("%s/documents/%s/binary", applicationParams.getDocumentURL(), documentId),
                     capturedHttpGet.getURI().toString()
        );

        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }
}
