package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class CaseDataStoreServiceImpl implements CaseDataStoreService {
    @Override
    public CaseDocumentMetadata getCaseDocumentMetadata(String caseReference, UUID documentId) {
        Document document = Document.builder().permissions(Arrays.asList(Permission.CREATE,Permission.READ)).id(documentId.toString()).build();
        return  CaseDocumentMetadata.builder().caseId(caseReference).documents(Optional.of(Arrays.asList(document))).build();

    }
}
