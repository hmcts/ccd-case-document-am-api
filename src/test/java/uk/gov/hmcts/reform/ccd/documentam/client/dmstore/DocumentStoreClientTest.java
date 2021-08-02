package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import io.vavr.control.Either;
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
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.vavr.api.VavrAssertions.assertThat;
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

@ExtendWith(MockitoExtension.class)
class DocumentStoreClientTest implements TestFixture {
    private static final String DM_STORE_URL = "http://localhost:4506";
    private static final HttpEntity<Object> NULL_REQUEST_ENTITY = null;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private DocumentStoreClient underTest;

    @BeforeEach
    void prepare() {
        doReturn(DM_STORE_URL).when(applicationParams).getDocumentURL();
    }

    @Test
    void testShouldSuccessfullyFetchDocument() {
        // GIVEN
        final Document expectedDocument = Document.builder().build();

        doReturn(expectedDocument)
            .when(restTemplate).getForObject(anyString(), ArgumentMatchers.<Class<Document>>any());

        // WHEN
        final Either<RuntimeException, Document> actualResult = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(actualResult)
            .isRight()
            .hasRightValueSatisfying(actualDocument -> assertThat(actualDocument).isEqualTo(expectedDocument));

        verify(restTemplate).getForObject(
            DM_STORE_URL + "/documents/" + DOCUMENT_ID,
            Document.class
        );
    }

    @Test
    void testShouldReturnExceptionWhenGetDocumentReturnsNotFound() {
        // GIVEN
        final HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpClientErrorException)
            .when(restTemplate).getForObject(anyString(), ArgumentMatchers.<Class<Document>>any());

        // WHEN
        final Either<RuntimeException, Document> actualResult = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(actualResult)
            .isLeft()
            .hasLeftValueSatisfying(actualException -> {
                final String message = String.format("Meta data does not exist for documentId: %s", DOCUMENT_ID);
                assertThat(actualException.getMessage()).isEqualTo(message);
                assertThat(actualException.getCause()).isInstanceOf(HttpClientErrorException.class);
            });

        verify(restTemplate).getForObject(
            DM_STORE_URL + "/documents/" + DOCUMENT_ID,
            Document.class
        );
    }

    @Test
    void testShouldRaiseExceptionWhenGetDocumentFails() {
        // GIVEN
        final HttpClientErrorException expectedException = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        doThrow(expectedException)
            .when(restTemplate).getForObject(anyString(), ArgumentMatchers.<Class<Document>>any());

        // WHEN
        final Either<RuntimeException, Document> result = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(result)
            .isLeft()
            .hasLeftValueSatisfying(left -> assertThat(left).isEqualTo(expectedException));

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
        verify(restTemplate)
            .exchange(DM_STORE_URL + "/documents/" + DOCUMENT_ID + "/binary",
                      HttpMethod.GET,
                      NULL_REQUEST_ENTITY,
                      ByteArrayResource.class);
    }

    @Test
    void testShouldSuccessfullyDeleteDocument() {
        // GIVEN
        final boolean permanent = true;
        doNothing().when(restTemplate).delete(anyString());

        // WHEN
        underTest.deleteDocument(DOCUMENT_ID, permanent);

        // THEN
        verify(restTemplate)
            .delete(String.format("%s/documents/%s?permanent=%s", DM_STORE_URL, DOCUMENT_ID, permanent));
    }

    @Test
    void testShouldRaiseExceptionWhenDocumentToDeleteIsNotFound() {
        // GIVEN
        final boolean permanent = true;
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(restTemplate)
            .delete(String.format("%s/documents/%s?permanent=%s", DM_STORE_URL, DOCUMENT_ID, permanent));

        // WHEN/THEN
        assertThatExceptionOfType(ResourceNotFoundException.class)
            .isThrownBy(() -> underTest.deleteDocument(DOCUMENT_ID, permanent))
            .withMessage("Resource not found " + DOCUMENT_ID);

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
        final Either<RuntimeException, PatchDocumentResponse> result =
            underTest.patchDocument(DOCUMENT_ID, dmTtlRequest);

        // THEN
        assertThat(result)
            .isRight()
            .hasRightValueSatisfying(right -> assertThat(right).isEqualTo(expectedResponse));

        verify(restTemplate).patchForObject(
            String.format("%s/documents/%s", DM_STORE_URL, DOCUMENT_ID),
            dmTtlRequest,
            PatchDocumentResponse.class
        );
    }

    @Test
    void testShouldReturnLeftWhenPatchDocumentReturnsNotFound() {
        // GIVEN
        final DmTtlRequest dmTtlRequest = buildTtlRequest();
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(restTemplate)
            .patchForObject(anyString(),
                            ArgumentMatchers.<Class<DmTtlRequest>>any(),
                            ArgumentMatchers.<Class<PatchDocumentResponse>>any());

        // WHEN
        final Either<RuntimeException, PatchDocumentResponse> result =
            underTest.patchDocument(DOCUMENT_ID, dmTtlRequest);

        // THEN
        assertThat(result)
            .isLeft()
            .hasLeftValueSatisfying(actualException -> {
                assertThat(actualException.getMessage()).isEqualTo("Resource not found " + DOCUMENT_ID);
                assertThat(actualException.getCause()).isInstanceOf(HttpClientErrorException.class);
            });

        verify(restTemplate).patchForObject(
            String.format("%s/documents/%s", DM_STORE_URL, DOCUMENT_ID),
            dmTtlRequest,
            PatchDocumentResponse.class
        );
    }

    @Test
    void testShouldReturnLeftWhenPatchDocumentFails() {
        // GIVEN
        final DmTtlRequest dmTtlRequest = buildTtlRequest();
        final HttpClientErrorException expectedException = new HttpClientErrorException(HttpStatus.BAD_GATEWAY);

        doThrow(expectedException).when(restTemplate)
            .patchForObject(anyString(),
                            ArgumentMatchers.<Class<DmTtlRequest>>any(),
                            ArgumentMatchers.<Class<PatchDocumentResponse>>any());

        // WHEN
        final Either<RuntimeException, PatchDocumentResponse> result =
            underTest.patchDocument(DOCUMENT_ID, dmTtlRequest);

        // THEN
        assertThat(result)
            .isLeft()
            .hasLeftValueSatisfying(left -> assertThat(left).isEqualTo(expectedException));

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

}
