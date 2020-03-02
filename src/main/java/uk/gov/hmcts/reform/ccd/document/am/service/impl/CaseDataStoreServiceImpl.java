package uk.gov.hmcts.reform.ccd.document.am.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.UnauthorizedException;
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

    private static final Logger LOG = LoggerFactory.getLogger(CaseDataStoreServiceImpl.class);

    @Override
    public Optional<CaseDocumentMetadata> getCaseDocumentMetadata(String caseId, UUID documentId) {
        Document document = Document.builder().permissions(Arrays.asList(Permission.CREATE,Permission.READ)).id(documentId.toString()).build();

        Optional<CaseDocumentMetadata> caseDocumentMetadata = Optional.of(CaseDocumentMetadata.builder().caseId(caseId).document(Optional.of(document))
                                                              .build());
        if (!caseDocumentMetadata.get().getDocument().isPresent()) {
            LOG.error("Case Document doesn't exist for requested document id at CCD Data Store API Side " + HttpStatus.NOT_FOUND);
            throw new UnauthorizedException(documentId.toString());
        } else {
            return caseDocumentMetadata;
        }

    }
}
