package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.dto.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PendingAccessRoutesTest {
    @ParameterizedTest
    @ValueSource(strings = {"metadata", "binary", "token", "ttl", "delete"})
    void pendingDenialPrecedesAllDataAndMutationOperations(String operation) {
        final DocumentManagementService service = mock(DocumentManagementService.class);
        final SecurityUtils security = mock(SecurityUtils.class);
        final UUID id = UUID.randomUUID();
        final Document document = Document.builder()
            .metadata(Map.of("pending_access_controlled", "true")).build();
        when(service.getDocumentMetadata(id)).thenReturn(document);
        when(security.getUserInfo()).thenReturn(UserInfo.builder().uid("other").build());
        doThrow(new ForbiddenException("denied")).when(service).checkPendingDocumentAccess(document, "other");
        final CaseDocumentAmController controller = new CaseDocumentAmController(service, security,
            mock(ApplicationParams.class));
        assertThrows(ForbiddenException.class, () -> {
            switch (operation) {
                case "metadata" -> controller.getDocumentByDocumentId(id, "service");
                case "binary" -> controller.getDocumentBinaryContentByDocumentId(id, null, "service", Map.of());
                case "token" -> controller.generateHashCode(id, "service");
                case "ttl" -> controller.patchDocumentByDocumentId(id,
                    new UpdateTtlRequest(LocalDateTime.now().plusDays(1)), "service");
                case "delete" -> controller.deleteDocumentByDocumentId(id, false, "service");
                default -> throw new AssertionError(operation);
            }
        });
        verify(service, never()).getDocumentBinaryContent(any());
        verify(service, never()).streamDocumentBinaryContent(any(), any(), any());
        verify(service, never()).generateHashToken(any(), any(), any());
        verify(service, never()).patchDocument(any(), any());
        verify(service, never()).deleteDocument(any(), anyBoolean());
    }

    @Test
    void configuredXuiUploadPermissionCanWithdrawOnlyItsOwnersPendingGrant() throws Exception {
        final var services = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
            getClass().getResourceAsStream("/service_config.json"),
            uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices.class);
        final var documents = mock(uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DocumentStoreClient.class);
        final var cases = mock(uk.gov.hmcts.reform.ccd.documentam.client.datastore.CaseDataStoreClient.class);
        final var service = new uk.gov.hmcts.reform.ccd.documentam.service.impl.DocumentManagementServiceImpl(
            documents, cases, services, mock(ApplicationParams.class));
        final UUID id = UUID.randomUUID();
        final Document pending = Document.builder().createdBy("owner")
            .metadata(Map.of("pending_access_controlled", "true", "case_type_id", "type",
                "jurisdiction", "jurisdiction")).build();
        when(documents.getDocument(id)).thenReturn(io.vavr.control.Either.right(pending));
        final SecurityUtils security = mock(SecurityUtils.class);
        when(security.getServiceNameFromS2SToken("xui")).thenReturn("xui_webapp");
        when(security.getServiceNameFromS2SToken("read-only")).thenReturn("em_npa_app");
        when(security.getUserInfo()).thenReturn(UserInfo.builder().uid("owner").build());
        final var controller = new CaseDocumentAmController(service, security, mock(ApplicationParams.class));
        org.junit.jupiter.api.Assertions.assertEquals(204, controller.revokePendingAccess(id, "xui")
            .getStatusCode().value());
        verify(documents).patchDocumentMetadataOnly(id, Map.of("pending_access_revoked", "true"));
        org.mockito.Mockito.clearInvocations(documents);
        assertThrows(ForbiddenException.class, () -> controller.revokePendingAccess(id, "read-only"));
        verify(documents, never()).patchDocumentMetadataOnly(any(), any());
        when(security.getUserInfo()).thenReturn(UserInfo.builder().uid("other").build());
        assertThrows(ForbiddenException.class, () -> controller.revokePendingAccess(id, "xui"));
        verify(documents, never()).patchDocumentMetadataOnly(any(), any());
        org.mockito.Mockito.verifyNoInteractions(cases);
    }

    @Test
    void pendingUploadEndpointForcesAccessControlEvenIfCallerOmitsTheFlag() {
        final DocumentManagementService service = mock(DocumentManagementService.class);
        final CaseDocumentAmController controller = new CaseDocumentAmController(service, mock(SecurityUtils.class),
            mock(ApplicationParams.class));
        final var request = new uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest(
            java.util.List.of(), "PUBLIC", "type", "jurisdiction");
        request.setCaseId("1234567890123456");
        controller.uploadPendingDocuments(request, mock(org.springframework.validation.BindingResult.class), "service");
        org.junit.jupiter.api.Assertions.assertTrue(request.isPendingAccess());
        verify(service).uploadDocuments(request);
    }

    @Test
    void legacyUploadEndpointCannotSilentlyIgnorePendingFlags() {
        final DocumentManagementService service = mock(DocumentManagementService.class);
        final CaseDocumentAmController controller = new CaseDocumentAmController(service, mock(SecurityUtils.class),
            mock(ApplicationParams.class));
        final var request = new uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest(
            java.util.List.of(), "PUBLIC", "type", "jurisdiction");
        request.setPendingAccess(true);
        assertThrows(uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException.class,
            () -> controller.uploadDocuments(request, mock(org.springframework.validation.BindingResult.class),
                "service"));
        org.mockito.Mockito.verifyNoInteractions(service);
    }
}
