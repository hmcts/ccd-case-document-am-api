package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;

class AuditLogFormatterTest implements TestFixture {

    private static final String AUDIT_LOG_TAG = "LA-CDAM";

    private AuditLogFormatter underTest;

    @BeforeEach
    void setUp() {
        underTest = new AuditLogFormatter(0);
    }

    @Test
    @DisplayName("Should have correct tagging")
    void shouldHaveCorrectTagging() {
        // GIVEN
        AuditEntry auditEntry = new AuditEntry();

        // WHEN
        final String result = underTest.format(auditEntry);

        // THEN
        assertThat(result).isNotNull().startsWith(AUDIT_LOG_TAG);
    }

    @Test
    @DisplayName("Should have correct labels")
    void shouldHaveCorrectLabels() {

        // GIVEN
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setDateTime("2021-04-26 15:39:45");
        auditEntry.setOperationType("TEST_OPERATION_TYPE");
        auditEntry.setIdamId("test_idamId");
        auditEntry.setInvokingService("test_invokingService");
        auditEntry.setHttpMethod(HttpMethod.GET.name());
        auditEntry.setHttpStatus(HttpStatus.OK.value());
        auditEntry.setRequestPath(REQUEST_PATH);
        auditEntry.setDocumentIds(List.of(DOCUMENT_ID.toString()));
        auditEntry.setJurisdiction(JURISDICTION);
        auditEntry.setCaseId(CASE_ID_VALUE);
        auditEntry.setCaseType(CASE_TYPE);
        auditEntry.setRequestId(REQUEST_ID);

        // WHEN
        final String result = underTest.format(auditEntry);

        // THEN
        assertThat(result)
            .isNotNull()
            .isEqualTo(AUDIT_LOG_TAG + " "
                           + "dateTime:2021-04-26 15:39:45,"
                           + "operationType:TEST_OPERATION_TYPE,"
                           + "idamId:test_idamId,"
                           + "invokingService:test_invokingService,"
                           + "endpointCalled:GET " + REQUEST_PATH + ","
                           + "operationalOutcome:200,"
                           + "documentId:" + DOCUMENT_ID + ","
                           + "jurisdiction:" + JURISDICTION + ","
                           + "caseType:" + CASE_TYPE + ","
                           + "caseId:" + CASE_ID_VALUE + ","
                           + "X-Request-ID:" + REQUEST_ID);
    }

    @Test
    @DisplayName("Should not log pair if empty")
    void shouldNotLogPairIfEmpty() {

        // GIVEN
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setDateTime("2021-04-26 15:39:45");
        auditEntry.setHttpMethod(HttpMethod.GET.name());
        auditEntry.setHttpStatus(HttpStatus.OK.value());
        auditEntry.setRequestPath(REQUEST_PATH);

        // WHEN
        final String result = underTest.format(auditEntry);

        // THEN
        assertEquals(
            "Should only log supplied pairs",
            result,
            AUDIT_LOG_TAG + " "
                + "dateTime:2021-04-26 15:39:45,"
                + "endpointCalled:GET " + REQUEST_PATH + ","
                + "operationalOutcome:200"
        );
    }

    @Test
    @DisplayName("Should handle lists with comma")
    void shouldHandleListsWithComma() {

        // GIVEN
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setDateTime("2021-04-26 15:39:45");
        auditEntry.setHttpMethod(HttpMethod.GET.name());
        auditEntry.setHttpStatus(HttpStatus.OK.value());
        auditEntry.setRequestPath(REQUEST_PATH);
        auditEntry.setDocumentIds(List.of(DOCUMENT_ID_1.toString(), DOCUMENT_ID_2.toString()));
        auditEntry.setCaseId(CASE_ID_VALUE);

        // WHEN
        final String result = underTest.format(auditEntry);

        // THEN
        assertEquals(
            "Should handle ID lists with comma",
            result,
            AUDIT_LOG_TAG + " "
                + "dateTime:2021-04-26 15:39:45,"
                + "endpointCalled:GET " + REQUEST_PATH + ","
                + "operationalOutcome:200,"
                + "documentId:" + DOCUMENT_ID_1 + "," + DOCUMENT_ID_2 + ","
                + "caseId:" + CASE_ID_VALUE
        );
    }

    @Test
    @DisplayName("Should handle lists with limit")
    void shouldHandleListsWithLimit() {

        // GIVEN
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setDateTime("2021-04-26 15:39:45");
        auditEntry.setHttpMethod(HttpMethod.GET.name());
        auditEntry.setHttpStatus(HttpStatus.OK.value());
        auditEntry.setRequestPath(REQUEST_PATH);
        auditEntry.setDocumentIds(List.of(DOCUMENT_ID_1.toString(), DOCUMENT_ID_2.toString(), DOCUMENT_ID.toString()));
        auditEntry.setCaseId(CASE_ID_VALUE);

        int auditLogMaxListSize = 2;
        underTest = new AuditLogFormatter(auditLogMaxListSize);

        // WHEN
        final String result = underTest.format(auditEntry);

        // THEN
        assertEquals(
            "Should apply limit to ID lists",
            result,
            AUDIT_LOG_TAG + " "
                + "dateTime:2021-04-26 15:39:45,"
                + "endpointCalled:GET " + REQUEST_PATH + ","
                + "operationalOutcome:200,"
                + "documentId:" + DOCUMENT_ID_1 + "," + DOCUMENT_ID_2 + ","
                + "caseId:" + CASE_ID_VALUE
        );
    }

}
