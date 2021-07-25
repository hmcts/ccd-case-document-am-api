package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.buildPatchDocumentResponse;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.buildTtlRequest;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.ORIGINAL_FILE_NAME;

class DocumentStoreClientTest implements TestFixture {
    private static final String DM_STORE_URL = "http://localhost:4506";
    private static final HttpEntity<Object> NULL_REQUEST_ENTITY = null;

    @Mock
    private RestTemplate restTemplate;

    private DocumentStoreClient underTest;

    private final boolean permanent = true;

    @BeforeEach
    void prepare() {
        MockitoAnnotations.openMocks(this);

        underTest = new DocumentStoreClient(DM_STORE_URL, restTemplate);
    }

    @Test
    void testShouldSuccessfullyFetchDocument() {
        // GIVEN
        final Document expectedDocument = Document.builder().build();

        doReturn(expectedDocument)
            .when(restTemplate).getForObject(anyString(), ArgumentMatchers.<Class<Document>>any());

        // WHEN
        final Optional<Document> actualDocument = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(actualDocument)
            .hasValue(expectedDocument);

        verify(restTemplate).getForObject(
            DM_STORE_URL + "/documents/" + DOCUMENT_ID,
            Document.class
        );
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testShouldReturnEmptyWhenGetDocumentIsCalled() {
        // GIVEN
        final Document noDocument = null;

        doReturn(noDocument).when(restTemplate).getForObject(anyString(), ArgumentMatchers.<Class<Document>>any());

        // WHEN
        final Optional<Document> actualDocument = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(actualDocument)
            .isNotPresent();

        verify(restTemplate).getForObject(
            DM_STORE_URL + "/documents/" + DOCUMENT_ID,
            Document.class
        );
    }

    @Test
    void testShouldReturnEmptyWhenGetDocumentReturnsNotFound() {
        // GIVEN
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
            .when(restTemplate).getForObject(anyString(), ArgumentMatchers.<Class<Document>>any());

        // WHEN
        final Optional<Document> actualDocument = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(actualDocument)
            .isNotPresent();

        verify(restTemplate).getForObject(
            DM_STORE_URL + "/documents/" + DOCUMENT_ID,
            Document.class
        );
    }

    @Test
    void testShouldRaiseExceptionWhenGetDocumentFails() {
        // GIVEN
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
            .when(restTemplate).getForObject(anyString(), ArgumentMatchers.<Class<Document>>any());

        // WHEN/THEN
        assertThatExceptionOfType(ServiceException.class)
            .isThrownBy(() -> underTest.getDocument(DOCUMENT_ID));

        verify(restTemplate).getForObject(
            DM_STORE_URL + "/documents/" + DOCUMENT_ID,
            Document.class
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
            .satisfies(entity -> assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyRestExchangeByteArray();
    }

    @ParameterizedTest
    @MethodSource("provideHttpErrorForDocumentParameters")
    void getDocumentBinaryContent_Throws_Exception(final HttpStatus status,
                                                   final Class<Throwable> clazz,
                                                   final String msgPrefix) {
        doThrow(new HttpClientErrorException(status)).when(restTemplate)
            .exchange(anyString(), eq(HttpMethod.GET), any(), ArgumentMatchers.<Class<ByteArrayResource>>any());

        assertThatExceptionOfType(clazz)
            .isThrownBy(() -> underTest.getDocumentAsBinary(DOCUMENT_ID))
            .withMessage(msgPrefix + DOCUMENT_ID);

        verifyRestExchangeByteArray();
    }

    private void verifyRestExchangeByteArray() {
        verify(restTemplate)
            .exchange(DM_STORE_URL + "/documents/" + DOCUMENT_ID + "/binary",
                      HttpMethod.GET,
                      NULL_REQUEST_ENTITY,
                      ByteArrayResource.class);
    }

    @Test
    void testShouldSuccessfullyDeleteDocument() {
        // GIVEN
        doNothing().when(restTemplate).delete(anyString());

        // WHEN
        underTest.deleteDocument(DOCUMENT_ID, permanent);

        // THEN
        verify(restTemplate)
            .delete(String.format("%s/documents/%s?permanent=%s", DM_STORE_URL, DOCUMENT_ID, permanent));
    }

    @ParameterizedTest
    @MethodSource("provideHttpErrorForDocumentParameters")
    void testShouldRaiseExceptionWhenDeleteDocumentIsCalled(final HttpStatus status,
                                                            final Class<Throwable> clazz,
                                                            final String messagePrefix) {
        // GIVEN
        doThrow(new HttpClientErrorException(status)).when(restTemplate)
            .delete(String.format("%s/documents/%s?permanent=%s", DM_STORE_URL, DOCUMENT_ID, permanent));

        // WHEN/THEN
        assertThatExceptionOfType(clazz)
            .isThrownBy(() -> underTest.deleteDocument(DOCUMENT_ID, permanent))
            .withMessage(messagePrefix + DOCUMENT_ID);

        verify(restTemplate)
            .delete(String.format("%s/documents/%s?permanent=%s", DM_STORE_URL, DOCUMENT_ID, permanent));
    }

    @Test
    void testShouldSuccessfullyPatchDocument() {
        // GIVEN
        final DmTtlRequest dmTtlRequest = buildTtlRequest();
        final PatchDocumentResponse expectedResponse = buildPatchDocumentResponse();

        doReturn(expectedResponse).when(restTemplate)
            .patchForObject(anyString(),
                            ArgumentMatchers.<Class<DmTtlRequest>>any(),
                            ArgumentMatchers.<Class<PatchDocumentResponse>>any());

        // WHEN
        final Optional<PatchDocumentResponse> actualResponse = underTest.patchDocument(DOCUMENT_ID, dmTtlRequest);

        // THEN
        assertThat(actualResponse)
            .hasValue(expectedResponse);

        verify(restTemplate).patchForObject(
            String.format("%s/documents/%s", DM_STORE_URL, DOCUMENT_ID),
            dmTtlRequest,
            PatchDocumentResponse.class
        );
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testShouldReturnEmptyWhenPatchDocumentIsCalled() {
        // GIVEN
        final DmTtlRequest dmTtlRequest = buildTtlRequest();
        final PatchDocumentResponse emptyResponse = null;

        doReturn(emptyResponse).when(restTemplate)
            .patchForObject(anyString(),
                            ArgumentMatchers.<Class<DmTtlRequest>>any(),
                            ArgumentMatchers.<Class<PatchDocumentResponse>>any());

        // WHEN
        final Optional<PatchDocumentResponse> actualResponse = underTest.patchDocument(DOCUMENT_ID, dmTtlRequest);

        // THEN
        assertThat(actualResponse)
            .isNotPresent();

        verify(restTemplate).patchForObject(
            String.format("%s/documents/%s", DM_STORE_URL, DOCUMENT_ID),
            dmTtlRequest,
            PatchDocumentResponse.class
        );
    }

    @Test
    void testShouldReturnEmptyWhenPatchDocumentReturnsNotFound() {
        // GIVEN
        final DmTtlRequest dmTtlRequest = buildTtlRequest();
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(restTemplate)
            .patchForObject(anyString(),
                            ArgumentMatchers.<Class<DmTtlRequest>>any(),
                            ArgumentMatchers.<Class<PatchDocumentResponse>>any());

        // WHEN
        final Optional<PatchDocumentResponse> actualResponse = underTest.patchDocument(DOCUMENT_ID, dmTtlRequest);

        // THEN
        assertThat(actualResponse)
            .isNotPresent();

        verify(restTemplate).patchForObject(
            String.format("%s/documents/%s", DM_STORE_URL, DOCUMENT_ID),
            dmTtlRequest,
            PatchDocumentResponse.class
        );
    }

    @Test
    void testShouldRaiseExceptionWhenPatchDocumentFails() {
        // GIVEN
        final DmTtlRequest dmTtlRequest = buildTtlRequest();

        doThrow(new HttpClientErrorException(HttpStatus.BAD_GATEWAY)).when(restTemplate)
            .patchForObject(anyString(),
                            ArgumentMatchers.<Class<DmTtlRequest>>any(),
                            ArgumentMatchers.<Class<PatchDocumentResponse>>any());

        // WHEN/THEN
        assertThatExceptionOfType(ServiceException.class)
            .isThrownBy(() -> underTest.patchDocument(DOCUMENT_ID, dmTtlRequest));


        verify(restTemplate).patchForObject(
            String.format("%s/documents/%s", DM_STORE_URL, DOCUMENT_ID),
            dmTtlRequest,
            PatchDocumentResponse.class
        );
    }

    @Test
    void testShouldSuccessfullyPatchDocumentMetadata() {
        // GIVEN
        final UpdateDocumentsCommand updateDocumentsCommand = new UpdateDocumentsCommand(NULL_TTL, emptyList());

        doAnswer(invocation -> null).when(restTemplate)
            .patchForObject(anyString(),
                            ArgumentMatchers.<Class<UpdateDocumentsCommand>>any(),
                            ArgumentMatchers.<Class<Void>>any());

        // WHEN
        underTest.patchDocumentMetadata(updateDocumentsCommand);

        // THEN
        verify(restTemplate).patchForObject(
            String.format("%s/documents", DM_STORE_URL),
            updateDocumentsCommand,
            Void.class
        );
    }

    @Test
    void patchDocumentMetadata_Throws_ServiceException() {
        // GIVEN
        final UpdateDocumentsCommand updateDocumentsCommand = new UpdateDocumentsCommand(NULL_TTL, emptyList());

        doThrow(new HttpClientErrorException(HttpStatus.BAD_GATEWAY)).when(restTemplate)
            .patchForObject(anyString(),
                            ArgumentMatchers.<Class<UpdateDocumentsCommand>>any(),
                            ArgumentMatchers.<Class<Void>>any());

        assertThatExceptionOfType(ServiceException.class)
            .isThrownBy(() -> underTest.patchDocumentMetadata(updateDocumentsCommand))
            .withMessage("Exception occurred with operation");

        verify(restTemplate).patchForObject(
            String.format("%s/documents", DM_STORE_URL),
            updateDocumentsCommand,
            Void.class
        );
    }
}
