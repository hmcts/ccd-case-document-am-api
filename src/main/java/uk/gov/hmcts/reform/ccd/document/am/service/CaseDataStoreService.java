package uk.gov.hmcts.reform.ccd.document.am.service;

import java.util.Optional;
import java.util.UUID;

import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;

public interface CaseDataStoreService {

    /**
     * Root GET endpoint.
     *
     * @param caseReference 16-digit universally unique case reference
     * @param documentId Document Id
     * @return Optional containing Case Metadata when found; empty optional otherwise
     */
    Optional<CaseDocumentMetadata> getCaseDocumentMetadata(final String caseReference, UUID documentId, String authorization);
}
