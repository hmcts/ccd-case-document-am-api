package uk.gov.hmcts.reform.ccd.documentam.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DocumentStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import io.vavr.control.Either;
import uk.gov.hmcts.reform.ccd.documentam.client.datastore.CaseDataStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class CaseBoundPendingAccessTest {
    private static final String CASE_ID = "1234567890123456";
    private final CaseDataStoreClient cases = mock(CaseDataStoreClient.class);
    private DocumentManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocumentManagementServiceImpl(null, cases, null, null);
    }

    private Document pending(String owner, long ttlOffset) {
        return Document.builder().createdBy(owner)
            .ttl(Date.from(Instant.now().plusSeconds(ttlOffset)))
            .metadata(Map.of("pending_access_controlled", "true", "pending_case_id", CASE_ID,
                "case_type_id", "type", "jurisdiction", "jurisdiction"))
            .build();
    }

    @Test
    void ownerMustRetainCurrentCaseAccess() {
        service.checkPendingDocumentAccess(pending("owner", 60), "owner");
        verify(cases).checkCaseAccess(CASE_ID, "type", "jurisdiction");
    }

    @Test
    void otherUserCannotReadPendingDocumentEvenIfTheyCanReadTheCase() {
        assertThrows(ForbiddenException.class,
            () -> service.checkPendingDocumentAccess(pending("owner", 60), "other"));
        verifyNoInteractions(cases);
    }

    @Test
    void revokedOwnerCannotReadPendingDocument() {
        doThrow(new ForbiddenException("revoked")).when(cases)
            .checkCaseAccess(CASE_ID, "type", "jurisdiction");
        assertThrows(ForbiddenException.class,
            () -> service.checkPendingDocumentAccess(pending("owner", 60), "owner"));
    }

    @Test
    void expiredUploadCannotBeReadOrExtended() {
        assertThrows(ForbiddenException.class,
            () -> service.checkPendingDocumentAccess(pending("owner", -60), "owner"));
        verifyNoInteractions(cases);
    }

    @Test
    void missingCreatorFailsClosed() {
        assertThrows(ForbiddenException.class,
            () -> service.checkPendingDocumentAccess(pending(null, 60), "owner"));
    }

    @Test
    void privateCreationUploadDoesNotRequireASavedCase() {
        final Document document = Document.builder().createdBy("owner")
            .ttl(Date.from(Instant.now().plusSeconds(60)))
            .metadata(Map.of("pending_access_controlled", "true")).build();
        service.checkPendingDocumentAccess(document, "owner");
        verifyNoInteractions(cases);
    }

    @Test
    void attachedAuthorizationTakesPriorityOverPendingFlags() {
        final Document attached = pending("other", -60).toBuilder().metadata(Map.of(
            "case_id", CASE_ID, "pending_access_controlled", "true")).build();
        service.checkPendingDocumentAccess(attached, "owner");
        verifyNoInteractions(cases);
    }

    @Test
    void unmarkedDocumentsRetainExistingPolicy() {
        service.checkPendingDocumentAccess(Document.builder().build(), "other");
        verifyNoInteractions(cases);
    }

    @Test
    void caseBindingWithoutOptInFailsClosed() {
        final DocumentUploadRequest request = new DocumentUploadRequest(List.of(), "PUBLIC", "type", "jurisdiction");
        request.setCaseId(CASE_ID);
        assertThrows(ForbiddenException.class, () -> service.checkPendingUploadAccess(request));
        verifyNoInteractions(cases);
    }

    @Test
    void controlledUploadValidatesTheAuthoritativeCaseContext() {
        final DocumentUploadRequest request = new DocumentUploadRequest(List.of(), "PUBLIC", "type", "jurisdiction");
        request.setCaseId(CASE_ID);
        request.setPendingAccess(true);
        service.checkPendingUploadAccess(request);
        verify(cases).checkCaseAccess(CASE_ID, "type", "jurisdiction");
    }

    @Test
    void attachmentCannotSubstituteAnotherCase() {
        final DocumentStoreClient documents = mock(DocumentStoreClient.class);
        service = new DocumentManagementServiceImpl(documents, cases, null, null);
        final UUID id = UUID.randomUUID();
        when(documents.getDocument(id)).thenReturn(Either.right(pending("owner", 60)));
        final CaseDocumentsMetadata metadata = new CaseDocumentsMetadata("9999999999999999", "type", "jurisdiction",
            List.of(new DocumentHashToken(id, "hash")));
        assertThrows(ForbiddenException.class, () -> service.patchDocumentMetadata(metadata));
        verify(documents, never()).patchDocumentMetadata(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validAttachmentChecksFreshCaseAccessAndTransitionsToNormalCaseMetadata() {
        final DocumentStoreClient documents = mock(DocumentStoreClient.class);
        final uk.gov.hmcts.reform.ccd.documentam.ApplicationParams params =
            mock(uk.gov.hmcts.reform.ccd.documentam.ApplicationParams.class);
        when(params.getSalt()).thenReturn("synthetic-salt");
        service = new DocumentManagementServiceImpl(documents, cases, null, params);
        final UUID id = UUID.randomUUID();
        when(documents.getDocument(id)).thenReturn(Either.right(pending("owner", 60)));
        final String hash = service.generateHashToken(id, null, "jurisdiction", "type");
        service.patchDocumentMetadata(new CaseDocumentsMetadata(CASE_ID, "type", "jurisdiction",
            List.of(new DocumentHashToken(id, hash))));
        verify(cases).checkCaseAccess(CASE_ID, "type", "jurisdiction");
        final org.mockito.ArgumentCaptor<uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand> command =
            org.mockito.ArgumentCaptor.forClass(uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand.class);
        verify(documents).patchDocumentMetadata(command.capture());
        org.junit.jupiter.api.Assertions.assertNull(command.getValue().getTtl());
        org.junit.jupiter.api.Assertions.assertEquals(CASE_ID,
            command.getValue().getDocuments().getFirst().getMetadata().get("case_id"));
    }

}
