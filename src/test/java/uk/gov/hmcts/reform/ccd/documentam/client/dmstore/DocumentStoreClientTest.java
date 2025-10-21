package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.pool.PoolStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.vavr.api.VavrAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.buildPatchDocumentResponse;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.buildTtlRequest;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.ORIGINAL_FILE_NAME;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.RESOURCE_NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class DocumentStoreClientTest implements TestFixture {
    private static final String DM_STORE_URL = "http://localhost:4506";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private DocumentStoreClient underTest;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private PoolingHttpClientConnectionManager httpClientConnectionManager;

    @Mock
    private HttpServletResponse httpResponseOut;

    @Mock
    private ClassicHttpResponse httpClientResponse;

    private UUID documentId;
    private Map<String, String> requestHeaders;

    private PoolStats poolStats = new PoolStats(0, 0, 0, 0);

    @BeforeEach
    void prepare() {
        documentId = UUID.randomUUID();
        requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer token");

        final HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.add(Constants.SERVICE_AUTHORIZATION, "service_token");

        when(securityUtils.serviceAuthorizationHeaders()).thenReturn(authHeaders);
        when(securityUtils.getUserInfo()).thenReturn(UserInfo.builder().uid(USER_ID).build());

        doReturn(DM_STORE_URL).when(applicationParams).getDocumentURL();
    }

    @Test
    void testShouldSuccessfullyFetchDocument() {
        // GIVEN
        final Document expectedDocument = Document.builder().build();

        doReturn(ResponseEntity.ok().body(expectedDocument))
            .when(restTemplate)
            .exchange(
                anyString(),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<Void>>any(),
                ArgumentMatchers.<Class<Document>>any());

        // WHEN
        final Either<ResourceNotFoundException, Document> actualResult = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(actualResult)
            .isRight()
            .hasRightValueSatisfying(actualDocument -> assertThat(actualDocument).isEqualTo(expectedDocument));

        verify(restTemplate, times(1)).exchange(
            eq(String.format("%s/documents/%s", DM_STORE_URL, DOCUMENT_ID)),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Document.class)
        );
    }

    @Test
    void testShouldReturnExceptionWhenGetDocumentReturnsNotFound() {
        // GIVEN
        final HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpClientErrorException)
            .when(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<Void>>any(),
                ArgumentMatchers.<Class<Document>>any());

        // WHEN
        final Either<ResourceNotFoundException, Document> actualResult = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(actualResult)
            .isLeft()
            .hasLeftValueSatisfying(actualException -> {
                final String message = String.format("Meta data does not exist for documentId: %s", DOCUMENT_ID);
                assertThat(actualException.getMessage()).isEqualTo(message);
                assertThat(actualException.getCause()).isInstanceOf(HttpClientErrorException.class);
            });

        verify(restTemplate, times(1)).exchange(
            eq(String.format("%s/documents/%s", DM_STORE_URL, DOCUMENT_ID)),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Document.class)
        );
    }

    @Test
    void testShouldSuccessfullyFetchDocumentBinary() {
        // GIVEN
        HttpHeaders headers = new HttpHeaders();
        headers.add(ORIGINAL_FILE_NAME, "name");
        headers.add(CONTENT_DISPOSITION, "disp");
        headers.add(DATA_SOURCE, "source");
        headers.add(CONTENT_TYPE, "type");

        doReturn(new ResponseEntity<>(headers, HttpStatus.OK)).when(restTemplate)
            .exchange(anyString(), eq(HttpMethod.GET), any(), ArgumentMatchers.<Class<ByteArrayResource>>any());

        // WHEN
        final ResponseEntity<ByteArrayResource> responseEntity = underTest.getDocumentAsBinary(DOCUMENT_ID);

        // THEN
        assertThat(responseEntity)
            .isNotNull()
            .satisfies(entity -> {
                assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(entity.getHeaders()).isEqualTo(headers);
            });


        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Try_ResponseNotOK() {
        // GIVEN
        final ByteArrayResource byteArrayResource = mock(ByteArrayResource.class);
        doReturn(new ResponseEntity<>(byteArrayResource, HttpStatus.BAD_REQUEST)).when(restTemplate)
            .exchange(anyString(), eq(HttpMethod.GET), any(), ArgumentMatchers.<Class<ByteArrayResource>>any());

        // WHEN
        final ResponseEntity<ByteArrayResource> responseEntity = underTest.getDocumentAsBinary(DOCUMENT_ID);

        // THEN
        assertThat(responseEntity)
            .isNotNull()
            .satisfies(entity -> assertThat(entity.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyRestExchangeByteArray();
    }

    @Test
    void getDocumentBinaryContent_Throws_NotFoundException() {
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(restTemplate)
            .exchange(anyString(), eq(HttpMethod.GET), any(), ArgumentMatchers.<Class<ByteArrayResource>>any());

        assertThatExceptionOfType(ResourceNotFoundException.class)
            .isThrownBy(() -> underTest.getDocumentAsBinary(DOCUMENT_ID))
            .withMessage("Resource not found " + DOCUMENT_ID);

        verifyRestExchangeByteArray();
    }

    private void verifyRestExchangeByteArray() {
        verify(restTemplate, times(1)).exchange(
            eq(String.format("%s/documents/%s/binary", DM_STORE_URL, DOCUMENT_ID)),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ByteArrayResource.class)
        );
    }

    @Test
    void testShouldSuccessfullyDeleteDocument() {
        // GIVEN
        final boolean permanent = true;
        doReturn(ResponseEntity.ok().build()).when(restTemplate)
            .exchange(anyString(), eq(HttpMethod.DELETE), any(), ArgumentMatchers.<Class<Void>>any());

        // WHEN
        underTest.deleteDocument(DOCUMENT_ID, permanent);

        // THEN
        verify(restTemplate, times(1)).exchange(
            eq(String.format("%s/documents/%s?permanent=%s", DM_STORE_URL, DOCUMENT_ID, permanent)),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    void testShouldRaiseExceptionWhenDocumentToDeleteIsNotFound() {
        // GIVEN
        final boolean permanent = true;
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(restTemplate)
            .exchange(eq(String.format("%s/documents/%s?permanent=%s", DM_STORE_URL, DOCUMENT_ID, permanent)),
                      eq(HttpMethod.DELETE), any(), ArgumentMatchers.<Class<Void>>any());

        // WHEN/THEN
        assertThatExceptionOfType(ResourceNotFoundException.class)
            .isThrownBy(() -> underTest.deleteDocument(DOCUMENT_ID, permanent))
            .withMessage("Resource not found " + DOCUMENT_ID);

        verify(restTemplate, times(1)).exchange(
            eq(String.format("%s/documents/%s?permanent=%s", DM_STORE_URL, DOCUMENT_ID, permanent)),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    void testShouldSuccessfullyPatchDocument() {
        // GIVEN
        final DmTtlRequest dmTtlRequest = buildTtlRequest();
        final PatchDocumentResponse expectedResponse = buildPatchDocumentResponse();

        doReturn(expectedResponse).when(restTemplate)
            .patchForObject(anyString(),
                            any(),
                            ArgumentMatchers.<Class<PatchDocumentResponse>>any());

        // WHEN
        final Either<ResourceNotFoundException, PatchDocumentResponse> result =
            underTest.patchDocument(DOCUMENT_ID, dmTtlRequest);

        // THEN
        assertThat(result)
            .isRight()
            .hasRightValueSatisfying(right -> assertThat(right).isEqualTo(expectedResponse));

        verify(restTemplate).patchForObject(
            eq(String.format("%s/documents/%s", DM_STORE_URL, DOCUMENT_ID)),
            any(HttpEntity.class),
            eq(PatchDocumentResponse.class)
        );
    }

    @Test
    void testShouldReturnLeftWhenPatchDocumentReturnsNotFound() {
        // GIVEN
        final DmTtlRequest dmTtlRequest = buildTtlRequest();
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(restTemplate)
            .patchForObject(anyString(),
                            any(),
                            ArgumentMatchers.<Class<PatchDocumentResponse>>any());

        // WHEN
        final Either<ResourceNotFoundException, PatchDocumentResponse> result =
            underTest.patchDocument(DOCUMENT_ID, dmTtlRequest);

        // THEN
        assertThat(result)
            .isLeft()
            .hasLeftValueSatisfying(actualException -> {
                assertThat(actualException.getMessage()).isEqualTo("Resource not found " + DOCUMENT_ID);
                assertThat(actualException.getCause()).isInstanceOf(HttpClientErrorException.class);
            });

        verify(restTemplate).patchForObject(
            eq(String.format("%s/documents/%s", DM_STORE_URL, DOCUMENT_ID)),
            any(HttpEntity.class),
            eq(PatchDocumentResponse.class)
        );
    }

    @Test
    void testShouldSuccessfullyPatchDocumentMetadata() {
        // GIVEN
        final UpdateDocumentsCommand updateDocumentsCommand = new UpdateDocumentsCommand(NULL_TTL, emptyList());

        doAnswer(invocation -> null).when(restTemplate)
            .patchForObject(anyString(),
                            any(),
                            ArgumentMatchers.<Class<Void>>any());

        // WHEN
        underTest.patchDocumentMetadata(updateDocumentsCommand);

        // THEN
        verify(restTemplate).patchForObject(
            eq(String.format("%s/documents", DM_STORE_URL)),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    void testShouldSuccessfullyUploadDocuments() {
        final Document document = Document.builder()
            .size(1000L)
            .mimeType(MIME_TYPE)
            .originalDocumentName(ORIGINAL_DOCUMENT_NAME)
            .classification(Classification.PUBLIC)
            .links(TestFixture.getLinks())
            .build();

        final DmUploadResponse dmUploadResponse = DmUploadResponse.builder()
            .embedded(DmUploadResponse.Embedded.builder().documents(List.of(document)).build())
            .build();

        doReturn(1).when(applicationParams).getDocumentTtlInDays();
        doReturn(dmUploadResponse)
            .when(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(DmUploadResponse.class));

        final DocumentUploadRequest uploadRequest = new DocumentUploadRequest(
            List.of(new MockMultipartFile("afile", "some".getBytes())),
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
            JURISDICTION_ID_VALUE
        );

        final DmUploadResponse response = underTest.uploadDocuments(uploadRequest);

        assertThat(response)
            .isNotNull();
    }

    @Test
    void testShouldSuccessfullyUploadEmptyDocument() {
        final Document document = Document.builder()
            .size(0L)
            .mimeType(MIME_TYPE)
            .originalDocumentName(ORIGINAL_DOCUMENT_NAME)
            .classification(Classification.PUBLIC)
            .links(TestFixture.getLinks())
            .build();

        final DmUploadResponse dmUploadResponse = DmUploadResponse.builder()
            .embedded(DmUploadResponse.Embedded.builder().documents(List.of(document)).build())
            .build();

        doReturn(1).when(applicationParams).getDocumentTtlInDays();
        doReturn(dmUploadResponse)
            .when(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(DmUploadResponse.class));

        final DocumentUploadRequest uploadRequest = new DocumentUploadRequest(
            List.of(new MockMultipartFile("afile", "".getBytes())),
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
            JURISDICTION_ID_VALUE
        );

        final DmUploadResponse response = underTest.uploadDocuments(uploadRequest);

        assertThat(response)
            .isNotNull()
            .satisfies(entity -> {
                assertThat(entity.getEmbedded().getDocuments().size()).isEqualTo(1);
                assertThat(entity.getEmbedded().getDocuments().getFirst()).isEqualTo(document);
            });
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenDocumentNotFound() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(httpClient.executeOpen(eq(null), any(HttpGet.class), eq(null))).thenReturn(httpClientResponse);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            underTest.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        assertTrue(exception.getMessage().contains(documentId.toString()));
        assertTrue(exception.getMessage().contains(RESOURCE_NOT_FOUND));

        verify(httpClient).executeOpen(eq(null), any(HttpGet.class), eq(null));
    }

    @Test
    void shouldThrowHttpServerErrorExceptionForInternalServerError() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
        when(httpClient.executeOpen(eq(null), any(HttpGet.class), eq(null))).thenReturn(httpClientResponse);

        HttpServerErrorException exception = assertThrows(HttpServerErrorException.class, () ->
            underTest.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Failed to retrieve document with ID: " + documentId));

        verify(httpClient).executeOpen(eq(null), any(HttpGet.class), eq(null));
    }

    @Test
    void shouldThrowHttpServerErrorExceptionForBadGateway() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.BAD_GATEWAY.value());
        when(httpClient.executeOpen(eq(null), any(HttpGet.class), eq(null))).thenReturn(httpClientResponse);

        HttpServerErrorException exception = assertThrows(HttpServerErrorException.class, () ->
            underTest.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Failed to retrieve document with ID: " + documentId));

        verify(httpClient).executeOpen(eq(null), any(HttpGet.class), eq(null));
    }

    @Test
    void shouldThrowHttpServerErrorExceptionForServiceUnavailable() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE.value());
        when(httpClient.executeOpen(eq(null), any(HttpGet.class), eq(null))).thenReturn(httpClientResponse);

        HttpServerErrorException exception = assertThrows(HttpServerErrorException.class, () ->
            underTest.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Failed to retrieve document with ID: " + documentId));

        verify(httpClient).executeOpen(eq(null), any(HttpGet.class), eq(null));
    }

    @Test
    void shouldThrowHttpServerErrorExceptionForGatewayTimeout() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.GATEWAY_TIMEOUT.value());
        when(httpClient.executeOpen(eq(null), any(HttpGet.class), eq(null))).thenReturn(httpClientResponse);

        HttpServerErrorException exception = assertThrows(HttpServerErrorException.class, () ->
            underTest.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Failed to retrieve document with ID: " + documentId));

        verify(httpClient).executeOpen(eq(null), any(HttpGet.class), eq(null));
    }

    @Test
    void shouldThrowResponseStatusExceptionForForbiddenStatus() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.FORBIDDEN.value());
        when(httpClient.executeOpen(eq(null), any(HttpGet.class), eq(null))).thenReturn(httpClientResponse);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("Failed to retrieve document with ID: " + documentId));

        verify(httpClient).executeOpen(eq(null), any(HttpGet.class), eq(null));
    }

    @Test
    void shouldThrowResponseStatusExceptionForUnauthorizedStatus() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(httpClient.executeOpen(eq(null), any(HttpGet.class), eq(null))).thenReturn(httpClientResponse);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("Failed to retrieve document with ID: " + documentId));

        verify(httpClient).executeOpen(eq(null), any(HttpGet.class), eq(null));
    }

    @Test
    void shouldThrowResponseStatusExceptionForBadRequestStatus() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.BAD_REQUEST.value());
        when(httpClient.executeOpen(eq(null), any(HttpGet.class), eq(null))).thenReturn(httpClientResponse);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("Failed to retrieve document with ID: " + documentId));

        verify(httpClient).executeOpen(eq(null), any(HttpGet.class), eq(null));
    }

    @Test
    void shouldThrowResponseStatusExceptionForIOException() throws IOException {
        when(httpClientConnectionManager.getTotalStats()).thenReturn(poolStats);
        when(httpClient.executeOpen(eq(null), any(HttpGet.class), eq(null)))
            .thenThrow(new IOException("Connection failed"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.streamDocumentAsBinary(documentId, httpResponseOut, requestHeaders)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Error occurred while processing the request " + String.format("%s/documents/%s/binary",
                         DM_STORE_URL, documentId) + ": Connection failed", exception.getReason());
        assertInstanceOf(IOException.class, exception.getCause());
        assertEquals(0, underTest.getHttpClientOpenConnections());

        verify(httpClient).executeOpen(eq(null), any(HttpGet.class), eq(null));
    }


    @Test
    void shouldReturnDmUploadResponseForSuccessfulUpload() throws IOException {
        DmUploadResponse expectedResponse = createMockDmUploadResponse();
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.OK.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        org.apache.hc.core5.http.HttpEntity mockEntity = mock(org.apache.hc.core5.http.HttpEntity.class);
        InputStream mockInputStream = mock(InputStream.class);

        when(httpClientResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(mockInputStream);

        // Mock readAllBytes() to return actual byte data
        String mockResponseJson = "{\"status\":\"success\",\"documentId\":\"123\"}"; // or whatever JSON you expect
        when(mockInputStream.readAllBytes()).thenReturn(mockResponseJson.getBytes(StandardCharsets.UTF_8));

        DocumentStoreClient spyClient = spy(underTest);
        ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
        when(mockObjectMapper.readValue(eq(mockResponseJson), eq(DmUploadResponse.class)))
            .thenReturn(expectedResponse);
        setObjectMapperField(spyClient, mockObjectMapper);

        DocumentUploadRequest uploadRequest = createMockUploadRequest();
        DmUploadResponse result = spyClient.uploadDocumentsAsStream(uploadRequest);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
        verify(mockObjectMapper).readValue(eq(mockResponseJson), eq(DmUploadResponse.class));
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenResponseEntityIsNull() throws IOException {
        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        when(httpClientResponse.getCode()).thenReturn(HttpStatus.OK.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);
        when(httpClientResponse.getEntity()).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.uploadDocumentsAsStream(uploadRequest)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Empty response from server", exception.getReason());

        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowHttpServerErrorExceptionFor5xxErrors() throws IOException {
        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        when(httpClientResponse.getCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        HttpServerErrorException exception = assertThrows(HttpServerErrorException.class, () ->
            underTest.uploadDocumentsAsStream(uploadRequest)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Document upload failed due to server error"));

        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowHttpServerErrorExceptionForBadGatewayUpload() throws IOException {
        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        when(httpClientResponse.getCode()).thenReturn(HttpStatus.BAD_GATEWAY.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        HttpServerErrorException exception = assertThrows(HttpServerErrorException.class, () ->
            underTest.uploadDocumentsAsStream(uploadRequest)
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Document upload failed due to server error"));

        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowHttpServerErrorExceptionForServiceUnavailableUpload() throws IOException {
        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        when(httpClientResponse.getCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        HttpServerErrorException exception = assertThrows(HttpServerErrorException.class, () ->
            underTest.uploadDocumentsAsStream(uploadRequest)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Document upload failed due to server error"));

        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowResponseStatusExceptionForClientErrors() throws IOException {
        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        when(httpClientResponse.getCode()).thenReturn(HttpStatus.BAD_REQUEST.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.uploadDocumentsAsStream(uploadRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("Document upload failed with status: " + HttpStatus.BAD_REQUEST));

        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowResponseStatusExceptionForUnauthorizedUpload() throws IOException {
        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        when(httpClientResponse.getCode()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.uploadDocumentsAsStream(uploadRequest)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("Document upload failed with status: " + HttpStatus.UNAUTHORIZED));

        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowResponseStatusExceptionForUploadIOException() throws IOException {
        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        when(httpClientConnectionManager.getTotalStats()).thenReturn(poolStats);
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null)))
            .thenThrow(new IOException("Network error"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            underTest.uploadDocumentsAsStream(uploadRequest)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Error occurred while processing the request " + String.format("%s/documents", DM_STORE_URL)
                         + ": Network error", exception.getReason());
        assertInstanceOf(IOException.class, exception.getCause());
        assertEquals(0, underTest.getHttpClientOpenConnections());

        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowHttpClientErrorExceptionForUnprocessableEntityWithResponseBody() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        org.apache.hc.core5.http.HttpEntity mockEntity = mock(org.apache.hc.core5.http.HttpEntity.class);
        String errorResponseJson = "{\"error\":\"Validation failed\",\"field\":\"documentType\","
            + "\"message\":\"Invalid document type\"}";
        ByteArrayInputStream errorInputStream =
            new ByteArrayInputStream(errorResponseJson.getBytes(StandardCharsets.UTF_8));

        when(httpClientResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(errorInputStream);

        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.UnprocessableEntity.class,
                                                          () -> underTest.uploadDocumentsAsStream(uploadRequest));

        String expectedMessage = exception.getResponseBodyAsString();
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals(errorResponseJson, expectedMessage);
        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowHttpClientErrorExceptionForUnprocessableEntityWithNullResponseBody() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        org.apache.hc.core5.http.HttpEntity mockEntity = mock(org.apache.hc.core5.http.HttpEntity.class);
        ByteArrayInputStream emptyInputStream = new ByteArrayInputStream(new byte[0]);

        when(httpClientResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(emptyInputStream);

        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                                                          () -> underTest.uploadDocumentsAsStream(uploadRequest));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals("422 Unprocessable Entity", exception.getMessage());
        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowHttpClientErrorExceptionForUnprocessableEntityWithEmptyResponseBody() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        org.apache.hc.core5.http.HttpEntity mockEntity = mock(org.apache.hc.core5.http.HttpEntity.class);
        String emptyResponse = "   ";
        ByteArrayInputStream emptyInputStream =
            new ByteArrayInputStream(emptyResponse.getBytes(StandardCharsets.UTF_8));

        when(httpClientResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(emptyInputStream);

        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                                                          () -> underTest.uploadDocumentsAsStream(uploadRequest));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowHttpClientErrorExceptionForUnprocessableEntityWithNoEntity() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        when(httpClientResponse.getEntity()).thenReturn(null);

        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                                                          () -> underTest.uploadDocumentsAsStream(uploadRequest));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals("422 Unprocessable Entity", exception.getMessage());
        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenJsonParsingFailsForSuccessResponse() throws IOException {
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.OK.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        org.apache.hc.core5.http.HttpEntity mockEntity = mock(org.apache.hc.core5.http.HttpEntity.class);
        String invalidJson = "{\"documentId\":\"123\",\"status\":\"success\",\"invalid\":}";
        ByteArrayInputStream invalidJsonStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

        when(httpClientResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(invalidJsonStream);

        DocumentStoreClient spyClient = spy(underTest);
        ObjectMapper mockObjectMapper = mock(ObjectMapper.class);

        JsonProcessingException jsonException = new JsonMappingException(null, "Invalid JSON structure");
        when(mockObjectMapper.readValue(eq(invalidJson), eq(DmUploadResponse.class)))
            .thenThrow(jsonException);

        setObjectMapperField(spyClient, mockObjectMapper);
        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                         () -> spyClient.uploadDocumentsAsStream(uploadRequest));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Failed to parse server response", exception.getReason());
        assertEquals(jsonException, exception.getCause());

        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
        verify(mockObjectMapper).readValue(eq(invalidJson), eq(DmUploadResponse.class));
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenJsonParsingFailsWithRealObjectMapper() throws IOException {
        // Given
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.OK.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        org.apache.hc.core5.http.HttpEntity mockEntity = mock(org.apache.hc.core5.http.HttpEntity.class);
        String malformedJson = "{\"documentId\":\"123\",\"status\":\"success\",\"timestamp\":invalid_date,"
            + "\"data\":[1,2,}";
        ByteArrayInputStream malformedJsonStream =
            new ByteArrayInputStream(malformedJson.getBytes(StandardCharsets.UTF_8));

        when(httpClientResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(malformedJsonStream);

        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                         () -> underTest.uploadDocumentsAsStream(uploadRequest));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Failed to parse server response", exception.getReason());
        assertNotNull(exception.getCause());
        assertInstanceOf(JsonProcessingException.class, exception.getCause());

        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    @Test
    void shouldSuccessfullyParseValidJsonResponse() throws IOException {
        DmUploadResponse expectedResponse = createMockDmUploadResponse();
        when(httpClientResponse.getCode()).thenReturn(HttpStatus.OK.value());
        when(httpClient.executeOpen(eq(null), any(HttpPost.class), eq(null))).thenReturn(httpClientResponse);

        org.apache.hc.core5.http.HttpEntity mockEntity = mock(org.apache.hc.core5.http.HttpEntity.class);

        ObjectMapper realObjectMapper = new ObjectMapper();
        String validJson = realObjectMapper.writeValueAsString(expectedResponse);
        ByteArrayInputStream validJsonStream = new ByteArrayInputStream(validJson.getBytes(StandardCharsets.UTF_8));

        when(httpClientResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(validJsonStream);

        DocumentUploadRequest uploadRequest = createMockUploadRequest();

        DmUploadResponse result = underTest.uploadDocumentsAsStream(uploadRequest);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(httpClient).executeOpen(eq(null), any(HttpPost.class), eq(null));
    }

    private DocumentUploadRequest createMockUploadRequest() {
        MultipartFile mockFile = mock(MultipartFile.class);
        try {
            when(mockFile.getInputStream()).thenReturn(mock(InputStream.class));
            when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new DocumentUploadRequest(
            List.of(mockFile),
            "PUBLIC",
            "Benefit",
            "SSCS"
        );
    }

    private DmUploadResponse createMockDmUploadResponse() {
        return DmUploadResponse.builder().build();
    }

    private void setObjectMapperField(DocumentStoreClient client, ObjectMapper mockObjectMapper) {
        try {
            Field objectMapperField = DocumentStoreClient.class.getDeclaredField("objectMapper");
            objectMapperField.setAccessible(true);
            objectMapperField.set(client, mockObjectMapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set objectMapper field", e);
        }
    }
}
