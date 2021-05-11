package uk.gov.hmcts.reform.ccd.documentam.auditlog.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.stereotype.Controller;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.AuditOperationType;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.LogAudit;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditAspectTest implements TestFixture {

    private final AuditAspect aspect = new AuditAspect();
    private TestController controllerProxy;

    @BeforeEach
    void setUp() {
        final AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(new TestController());
        aspectJProxyFactory.addAspect(aspect);

        final DefaultAopProxyFactory proxyFactory = new DefaultAopProxyFactory();
        final AopProxy aopProxy = proxyFactory.createAopProxy(aspectJProxyFactory);

        controllerProxy = (TestController) aopProxy.getProxy();
    }

    @Test
    @DisplayName("Should populate audit context when single IDs")
    void shouldPopulateAuditContextWhenSingleIds() {
        // WHEN
        controllerProxy.getDocumentByDocumentId_LogSingleId(RANDOM_DOCUMENT_ID);
        final AuditContext context = AuditContextHolder.getAuditContext();

        // THEN
        assertThat(context)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getAuditOperationType()).isEqualTo(AuditOperationType.DOWNLOAD_DOCUMENT_BY_ID);
                assertThat(x.getDocumentIds()).singleElement().isEqualTo(RANDOM_DOCUMENT_ID);
            });

    }

    @Test
    @DisplayName("Should populate audit context when single and list of IDs")
    void shouldPopulateAuditContextWhenSingleAndListOfIds() {

        // GIVEN
        final UUID id1 = UUID.randomUUID();
        final UUID id2 = UUID.randomUUID();

        final List<DocumentHashToken> documentHashTokens = List.of(
            DocumentHashToken.builder()
                .id(id1.toString())
                .build(),
            DocumentHashToken.builder()
                .id(id2.toString())
                .build()
        );

        // WHEN
        controllerProxy.patchMetaDataOnDocuments_LogListOfIds(
            CaseDocumentsMetadata.builder()
                .caseId(CASE_ID_VALID_1)
                .documentHashTokens(documentHashTokens)
                .build()
        );
        final AuditContext context = AuditContextHolder.getAuditContext();

        // THEN
        assertThat(context)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getAuditOperationType()).isEqualTo(AuditOperationType.PATCH_METADATA_ON_DOCUMENTS);
                assertThat(x.getDocumentIds()).isEqualTo(List.of(id1.toString(), id2.toString()));
                assertThat(x.getCaseIds()).isEqualTo(List.of(CASE_ID_VALID_1));
            });

    }

    @Controller
    @SuppressWarnings("unused")
    static class TestController {

        @LogAudit(
            operationType = AuditOperationType.DOWNLOAD_DOCUMENT_BY_ID,
            documentId = "#documentId"
        )
        public void getDocumentByDocumentId_LogSingleId(final String documentId) {
        }

        @LogAudit(
            operationType = AuditOperationType.PATCH_METADATA_ON_DOCUMENTS,
            documentIds = "T(uk.gov.hmcts.reform.ccd.documentam.util.DocumentIdsExtractor)"
                + ".extractIds(#caseDocumentsMetadata.documentHashTokens)",
            caseId = "#caseDocumentsMetadata.caseId"
        )
        public void patchMetaDataOnDocuments_LogListOfIds(final CaseDocumentsMetadata caseDocumentsMetadata) {
        }

    }
}
