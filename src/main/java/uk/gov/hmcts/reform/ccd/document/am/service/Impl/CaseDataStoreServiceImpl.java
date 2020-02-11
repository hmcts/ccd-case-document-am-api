package uk.gov.hmcts.reform.ccd.document.am.service.Impl;

import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;

import java.util.UUID;

public class CaseDataStoreServiceImpl implements CaseDataStoreService {
    @Override
    public CaseDocumentMetadata getCaseDocumentMetadata(String caseReference, UUID documentId) {
        return null;
    }
}
