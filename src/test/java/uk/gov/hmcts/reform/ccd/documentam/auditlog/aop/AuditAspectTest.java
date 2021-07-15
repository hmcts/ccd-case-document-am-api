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
        controllerProxy.getDocumentByDocumentId_LogSingleId(DOCUMENT_ID.toString());
        final AuditContext context = AuditContextHolder.getAuditContext();

        // THEN
        assertThat(context)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getAuditOperationType()).isEqualTo(AuditOperationType.DOWNLOAD_DOCUMENT_BY_ID);
                assertThat(x.getDocumentIds()).singleElement().isEqualTo(DOCUMENT_ID.toString());
            });

    }

    @Test
    @DisplayName("Should populate audit context when single and list of IDs")
    void shouldPopulateAuditContextWhenSingleAndListOfIds() {

        final List<DocumentHashToken> documentHashTokens = List.of(
            DocumentHashToken.builder()
                .id(DOCUMENT_ID_1)
                .build(),
            DocumentHashToken.builder()
                .id(DOCUMENT_ID_2)
                .build()
        );

        // WHEN
        controllerProxy.patchMetaDataOnDocuments_LogListOfIds(
            CaseDocumentsMetadata.builder()
                .caseId(CASE_ID_VALUE)
                .documentHashTokens(documentHashTokens)
                .build()
        );
        final AuditContext context = AuditContextHolder.getAuditContext();

        // THEN
        assertThat(context)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getAuditOperationType()).isEqualTo(AuditOperationType.PATCH_METADATA_ON_DOCUMENTS);
                assertThat(x.getDocumentIds()).isEqualTo(List.of(DOCUMENT_ID_1.toString(), DOCUMENT_ID_2.toString()));
                assertThat(x.getCaseId()).isEqualTo(CASE_ID_VALUE);
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
