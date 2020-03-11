package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;

class CaseDataStoreServiceImplTest {

    private static final String CASE_ID = "1582550122096256";
    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";

    private CaseDataStoreServiceImpl sut = new CaseDataStoreServiceImpl();

    Optional<CaseDocumentMetadata> caseDocumentMetadata;

    @Test
    void getCaseDocumentMetadataSuccess() {
        caseDocumentMetadata = sut.getCaseDocumentMetadata(CASE_ID, getUuid(MATCHED_DOCUMENT_ID));
        assertNotNull(caseDocumentMetadata);
        assertEquals(CASE_ID,caseDocumentMetadata.get().getCaseId());
        assertEquals(MATCHED_DOCUMENT_ID,caseDocumentMetadata.get().getDocument().get().getId());
    }

    private UUID getUuid(String id) {
        return UUID.fromString(id);
    }
}
