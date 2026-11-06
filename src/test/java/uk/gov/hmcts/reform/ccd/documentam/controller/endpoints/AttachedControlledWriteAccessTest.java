package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.client.datastore.CaseDataStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DocumentStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.dto.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.impl.DocumentManagementServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachedControlledWriteAccessTest {
    @ParameterizedTest
    @CsvSource({"ttl,false", "delete,false", "ttl,true", "delete,true"})
    void documentWritePermissionIsRequiredEvenWhenTheServiceCanUpdate(String operation, boolean permitted)
        throws Exception {
        final var services = new ObjectMapper().readValue(getClass().getResourceAsStream("/service_config.json"),
            AuthorisedServices.class);
        final var documents = mock(DocumentStoreClient.class);
        final var cases = mock(CaseDataStoreClient.class);
        final var service = new DocumentManagementServiceImpl(documents, cases, services,
            mock(ApplicationParams.class));
        final UUID id = UUID.randomUUID();
        final String caseId = "1234567890123456";
        final Document document = Document.builder().metadata(Map.of("case_id", caseId,
            "case_type_id", "Asylum", "jurisdiction", "IA", "pending_access_controlled", "true")).build();
        when(documents.getDocument(id)).thenReturn(Either.right(document));
        final Permission required = operation.equals("ttl") ? Permission.UPDATE : Permission.DELETE;
        when(cases.getCaseDocumentMetadata(caseId, id)).thenReturn(Optional.of(DocumentPermissions.builder().id(id)
            .permissions(permitted ? List.of(Permission.READ, required) : List.of(Permission.READ)).build()));
        when(documents.patchDocument(any(), any())).thenReturn(Either.right(PatchDocumentResponse.builder().build()));
        final var security = mock(SecurityUtils.class);
        when(security.getServiceNameFromS2SToken("service")).thenReturn("iac");
        final var controller = new CaseDocumentAmController(service, security, mock(ApplicationParams.class));
        final org.junit.jupiter.api.function.Executable mutation = operation.equals("ttl")
            ? () -> controller.patchDocumentByDocumentId(id,
                new UpdateTtlRequest(LocalDateTime.now().plusDays(1)), "service")
            : () -> controller.deleteDocumentByDocumentId(id, false, "service");
        if (permitted) {
            assertDoesNotThrow(mutation);
        } else {
            assertThrows(ForbiddenException.class, mutation);
            verify(documents, never()).patchDocument(any(), any());
            verify(documents, never()).deleteDocument(any(), any());
        }
    }
}
