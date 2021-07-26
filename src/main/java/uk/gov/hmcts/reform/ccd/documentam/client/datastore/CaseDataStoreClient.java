package uk.gov.hmcts.reform.ccd.documentam.client.datastore;

import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;

import java.util.Optional;
import java.util.UUID;

public interface CaseDataStoreClient {
    String EXPERIMENTAL_HEADER = "experimental";

    /**
     * Root GET endpoint.
     *
     * @param caseReference 16-digit universally unique case reference
     * @param documentId Document Id
     * @return Optional containing Case Metadata when found; empty optional otherwise
     */
    Optional<DocumentPermissions> getCaseDocumentMetadata(String caseReference, UUID documentId);
}
