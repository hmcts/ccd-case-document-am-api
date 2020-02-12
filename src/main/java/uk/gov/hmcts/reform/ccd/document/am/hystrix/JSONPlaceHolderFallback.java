package uk.gov.hmcts.reform.ccd.document.am.hystrix;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;

import java.util.Optional;
import java.util.UUID;

@Component
public class JSONPlaceHolderFallback {



    public Optional<StoredDocumentHalResource> getDocumentMetadata(UUID documentId) {
        return Optional.empty();
    }
}
