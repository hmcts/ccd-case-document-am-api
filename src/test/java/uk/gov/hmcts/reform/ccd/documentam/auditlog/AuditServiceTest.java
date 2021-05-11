package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.aop.AuditContext;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest implements TestFixture {

    private static final String INVOKING_SERVICE = "Test Invoking Service";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS")
        .withZone(ZoneOffset.UTC);

    private final Clock fixedClock = Clock.fixed(Instant.parse("2021-04-28T14:42:32.08Z"), ZoneOffset.UTC);

    private final UserInfo userInfo = UserInfo.builder()
        .uid(CREATED_BY)
        .build();

    private AuditService underTest;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Captor
    ArgumentCaptor<AuditEntry> captor;

    @BeforeEach
    void setUp() {
        underTest = new AuditService(fixedClock, securityUtils, auditRepository);
    }

    @Test
    @DisplayName("should save to audit repository")
    void shouldSaveToAuditRepository() {

        // GIVEN
        final AuditContext auditContext = AuditContext.auditContextWith()
            .auditOperationType(AuditOperationType.DOWNLOAD_DOCUMENT_BINARY_CONTENT_BY_ID)
            .invokingService(INVOKING_SERVICE)
            .httpMethod(HttpMethod.GET.name())
            .httpStatus(HttpStatus.OK.value())
            .requestPath(REQUEST_PATH)
            .requestId(REQUEST_ID)
            .build();
        given(securityUtils.getUserInfo()).willReturn(userInfo);

        // WHEN
        underTest.audit(auditContext);

        // THEN
        verify(auditRepository).save(captor.capture());

        final AuditEntry auditEntry = captor.getValue();
        assertThat(auditEntry)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getDateTime()).isEqualTo(formatter.format(fixedClock.instant()));
                assertThat(x.getOperationType()).isEqualTo(auditContext.getAuditOperationType().getLabel());
                assertThat(x.getInvokingService()).isEqualTo(auditContext.getInvokingService());
                assertThat(x.getHttpMethod()).isEqualTo(auditContext.getHttpMethod());
                assertThat(x.getHttpStatus()).isEqualTo(auditContext.getHttpStatus());
                assertThat(x.getRequestPath()).isEqualTo(auditContext.getRequestPath());
                assertThat(x.getRequestId()).isEqualTo(auditContext.getRequestId());
            });

    }

    @Test
    @DisplayName("should still save to audit repository when null OperationType")
    void shouldSaveToAuditRepositoryWhenOperationTypeIsNull() {

        // GIVEN
        final AuditContext auditContext = AuditContext.auditContextWith()
            .auditOperationType(null)
            .httpStatus(HttpStatus.OK.value())
            .build();
        given(securityUtils.getUserInfo()).willReturn(userInfo);

        // WHEN
        underTest.audit(auditContext);

        // THEN
        verify(auditRepository).save(captor.capture());

        final AuditEntry auditEntry = captor.getValue();
        assertThat(auditEntry)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getHttpStatus()).isEqualTo(auditContext.getHttpStatus());
                assertThat(x.getOperationType()).isNullOrEmpty();
            });
    }

    @Test
    @DisplayName("should save to audit repository with IDAM/User ID")
    void shouldSaveToAuditRepositoryWithIdamId() {

        // GIVEN
        final AuditContext auditContext = AuditContext.auditContextWith()
            .httpStatus(HttpStatus.OK.value())
            .build();
        given(securityUtils.getUserInfo()).willReturn(userInfo);

        // WHEN
        underTest.audit(auditContext);

        // THEN
        verify(auditRepository).save(captor.capture());

        final AuditEntry auditEntry = captor.getValue();
        assertThat(auditEntry)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getHttpStatus()).isEqualTo(auditContext.getHttpStatus());
                assertThat(x.getIdamId()).isEqualTo(userInfo.getUid());
            });
    }

    @Test
    @DisplayName("should save to audit repository when IDs lists are provided")
    void shouldSaveToAuditRepositoryWhenListOfIds() {

        // GIVEN
        final List<String> documentIds = List.of(DOCUMENT_ID_1, DOCUMENT_ID_2);
        final List<String> caseIds = List.of(CASE_ID_VALID_1, CASE_ID_VALID_2);
        final AuditContext auditContext = AuditContext.auditContextWith()
            .documentIds(documentIds)
            .caseIds(caseIds)
            .build();
        given(securityUtils.getUserInfo()).willReturn(userInfo);

        // WHEN
        underTest.audit(auditContext);

        // THEN
        verify(auditRepository).save(captor.capture());

        final AuditEntry auditEntry = captor.getValue();
        assertThat(auditEntry)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getDocumentIds()).hasSameElementsAs(documentIds);
                assertThat(x.getCaseIds()).hasSameElementsAs(caseIds);
            });
    }

    @Test
    @DisplayName("should load invoking service from S2S token")
    void shouldLoadInvokingServiceFromS2SToken() {

        // GIVEN
        final String s2sToken = "Test Token";
        final String expectedInvokingServiceName = "Test InvokingService";
        final MockHttpServletRequest request = new MockHttpServletRequest(
            HttpMethod.GET.name(),
            REQUEST_PATH
        );
        request.addHeader(SecurityUtils.SERVICE_AUTHORIZATION, s2sToken);
        given(securityUtils.getServiceNameFromS2SToken(anyString())).willReturn(expectedInvokingServiceName);

        // WHEN
        final String response = underTest.getInvokingService(request);

        // THEN
        verify(securityUtils).getServiceNameFromS2SToken(s2sToken);
        assertThat(response)
            .isNotNull()
            .isEqualTo(expectedInvokingServiceName);

    }

}
