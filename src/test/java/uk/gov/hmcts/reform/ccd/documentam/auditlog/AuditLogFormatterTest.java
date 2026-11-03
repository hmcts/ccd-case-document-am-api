package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogFormatterTest implements TestFixture {

    private static final String AUDIT_LOG_TAG = "LA-CDAM";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AuditLogFormatter underTest;

    @BeforeEach
    void setUp() {
        underTest = new AuditLogFormatter(0);
    }

    @Test
    @DisplayName("Should have correct tagging")
    void shouldHaveCorrectTagging() throws Exception {
        // GIVEN
        AuditEntry auditEntry = new AuditEntry();

        // WHEN
        final String result = underTest.format(auditEntry);
        final JsonNode json = objectMapper.readTree(result);

        // THEN
        assertThat(json.get("tag").asString()).isEqualTo(AUDIT_LOG_TAG);
    }

    @Test
    @DisplayName("Should have correct labels")
    void shouldHaveCorrectLabels() throws Exception {

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
        final JsonNode json = objectMapper.readTree(result);

        // THEN
        assertThat(json.get("tag").asString()).isEqualTo(AUDIT_LOG_TAG);
        assertThat(json.get("dateTime").asString()).isEqualTo("2021-04-26 15:39:45");
        assertThat(json.get("operationType").asString()).isEqualTo("TEST_OPERATION_TYPE");
        assertThat(json.get("idamId").asString()).isEqualTo("test_idamId");
        assertThat(json.get("invokingService").asString()).isEqualTo("test_invokingService");
        assertThat(json.get("endpointCalled").asString()).isEqualTo("GET " + REQUEST_PATH);
        assertThat(json.get("operationalOutcome").asInt()).isEqualTo(200);
        assertThat(json.get("documentId").size()).isEqualTo(1);
        assertThat(json.get("documentId").get(0).asString()).isEqualTo(DOCUMENT_ID.toString());
        assertThat(json.get("jurisdiction").asString()).isEqualTo(JURISDICTION);
        assertThat(json.get("caseType").asString()).isEqualTo(CASE_TYPE);
        assertThat(json.get("caseId").asString()).isEqualTo(CASE_ID_VALUE);
        assertThat(json.get("X-Request-ID").asString()).isEqualTo(REQUEST_ID);
    }

    @Test
    @DisplayName("Should not log pair if empty")
    void shouldNotLogPairIfEmpty() throws Exception {

        // GIVEN
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setDateTime("2021-04-26 15:39:45");
        auditEntry.setHttpMethod(HttpMethod.GET.name());
        auditEntry.setHttpStatus(HttpStatus.OK.value());
        auditEntry.setRequestPath(REQUEST_PATH);

        // WHEN
        final String result = underTest.format(auditEntry);
        final JsonNode json = objectMapper.readTree(result);

        // THEN
        assertThat(json.get("tag").asString()).isEqualTo(AUDIT_LOG_TAG);
        assertThat(json.get("dateTime").asString()).isEqualTo("2021-04-26 15:39:45");
        assertThat(json.get("endpointCalled").asString()).isEqualTo("GET " + REQUEST_PATH);
        assertThat(json.get("operationalOutcome").asInt()).isEqualTo(200);
        assertThat(json.has("operationType")).isFalse();
        assertThat(json.has("documentId")).isFalse();
    }

    @Test
    @DisplayName("Should handle lists with comma")
    void shouldHandleListsWithComma() throws Exception {

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
        final JsonNode json = objectMapper.readTree(result);

        // THEN
        assertThat(json.get("documentId").size()).isEqualTo(2);
        assertThat(json.get("documentId").get(0).asString()).isEqualTo(DOCUMENT_ID_1.toString());
        assertThat(json.get("documentId").get(1).asString()).isEqualTo(DOCUMENT_ID_2.toString());
        assertThat(json.get("caseId").asString()).isEqualTo(CASE_ID_VALUE);
    }

    @Test
    @DisplayName("Should handle lists with limit")
    void shouldHandleListsWithLimit() throws Exception {

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
        final JsonNode json = objectMapper.readTree(result);

        // THEN
        assertThat(json.get("documentId").size()).isEqualTo(2);
        assertThat(json.get("documentId").get(0).asString()).isEqualTo(DOCUMENT_ID_1.toString());
        assertThat(json.get("documentId").get(1).asString()).isEqualTo(DOCUMENT_ID_2.toString());
        assertThat(json.get("caseId").asString()).isEqualTo(CASE_ID_VALUE);
    }

}
