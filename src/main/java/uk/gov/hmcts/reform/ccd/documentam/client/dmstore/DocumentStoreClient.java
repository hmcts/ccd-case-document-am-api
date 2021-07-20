package uk.gov.hmcts.reform.ccd.documentam.client.dmstore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE;

@Named
public class DocumentStoreClient {

    private final String documentStoreBaseUrl;
    private final RestTemplate restTemplate;

    @Inject
    public DocumentStoreClient(@Value("${documentStoreUrl}") final String documentStoreBaseUrl,
                               final RestTemplate restTemplate) {
        this.documentStoreBaseUrl = documentStoreBaseUrl;
        this.restTemplate = restTemplate;
    }

    public Optional<Document> getDocument(final UUID documentId) {
        try {
            final Document document = restTemplate.getForObject(
                String.format("%s/documents/%s", documentStoreBaseUrl, documentId),
                Document.class
            );

            return Optional.ofNullable(document);
        } catch (HttpClientErrorException exception) {
            if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                return Optional.empty();
            }
            throw new ServiceException(
                String.format(EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE, documentId),
                exception
            );
        }
    }

}
