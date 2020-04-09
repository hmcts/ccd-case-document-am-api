package uk.gov.hmcts.reform.ccd.document.am.service;

import uk.gov.hmcts.reform.ccd.document.am.model.Document;

import java.util.Optional;
import java.util.UUID;

public interface CaseDataStoreService {

    /**
     * Root GET endpoint.
     *
     * @param caseReference 16-digit universally unique case reference
     * @param documentId Document Id
     * @return Optional containing Case Metadata when found; empty optional otherwise
     */
    Optional<Document> getCaseDocumentMetadata(final String caseReference, UUID documentId);
}
