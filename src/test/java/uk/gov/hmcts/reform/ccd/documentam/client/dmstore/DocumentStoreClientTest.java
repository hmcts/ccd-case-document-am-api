package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class DocumentStoreClientTest implements TestFixture {
    private static final String DM_STORE_URL = "http://localhost:4506";

    @Mock
    private RestTemplate restTemplate;

    private DocumentStoreClient underTest;

    @BeforeEach
    void prepare() {
        MockitoAnnotations.openMocks(this);

        underTest = new DocumentStoreClient(DM_STORE_URL, restTemplate);
    }

    @Test
    void testShouldFetchDocument() {
        // GIVEN
        final Document expectedDocument = Document.builder().build();

        doReturn(expectedDocument).when(restTemplate).getForObject(
            anyString(),
            ArgumentMatchers.<Class<Document>>any()
        );

        // WHEN
        final Optional<Document> actualDocument = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(actualDocument)
            .hasValue(expectedDocument);

        verify(restTemplate).getForObject(
            eq(DM_STORE_URL + "/documents/" + DOCUMENT_ID),
            ArgumentMatchers.<Class<Document>>any()
        );
    }

    @Test
    void testShouldEmptyResponse() {
        // GIVEN
        final Document noDocument = null;

        doReturn(noDocument).when(restTemplate).getForObject(
            anyString(),
            ArgumentMatchers.<Class<Document>>any()
        );

        // WHEN
        final Optional<Document> actualDocument = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(actualDocument)
            .isNotPresent();

        verify(restTemplate).getForObject(
            eq(DM_STORE_URL + "/documents/" + DOCUMENT_ID),
            ArgumentMatchers.<Class<Document>>any()
        );
    }

    @Test
    void testShouldRaiseResourceNotFoundExceptionWhenGetDocumentReturnsNotFound() {
        // GIVEN
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(restTemplate).getForObject(
            anyString(),
            ArgumentMatchers.<Class<Document>>any()
        );

        // WHEN
        final Optional<Document> actualDocument = underTest.getDocument(DOCUMENT_ID);

        // THEN
        assertThat(actualDocument)
            .isNotPresent();

        verify(restTemplate).getForObject(
            eq(DM_STORE_URL + "/documents/" + DOCUMENT_ID),
            ArgumentMatchers.<Class<Document>>any()
        );
    }

    @Test
    void testShouldRaiseExceptionWhenGetDocumentFails() {
        // GIVEN
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST)).when(restTemplate).getForObject(
            anyString(),
            ArgumentMatchers.<Class<Document>>any()
        );

        // WHEN
        assertThatExceptionOfType(ServiceException.class)
            .isThrownBy(() -> underTest.getDocument(DOCUMENT_ID));

        // THEN
        verify(restTemplate).getForObject(
            eq(DM_STORE_URL + "/documents/" + DOCUMENT_ID),
            ArgumentMatchers.<Class<Document>>any()
        );
    }
}
