package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.aop.AuditContext;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.aop.AuditContextHolder;
import uk.gov.hmcts.reform.ccd.documentam.configuration.AuditConfiguration;
import wiremock.org.eclipse.jetty.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AuditInterceptorTest implements TestFixture {

    private static final int STATUS_HIDDEN = HttpStatus.IM_A_TEAPOT_418;
    private static final int STATUS_NOT_HIDDEN = HttpStatus.OK_200;
    private static final String METHOD = "GET";

    private final AuditContext auditContext = new AuditContext();

    private final List<Integer> ignoreStatuses = List.of(STATUS_HIDDEN);

    private AuditInterceptor underTest;

    @Mock
    private AuditService auditService;

    @Mock
    private HandlerMethod handler;

    @Mock
    private LogAudit logAudit;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        final boolean enableAuditLog = true;
        underTest = new AuditInterceptor(auditService, enableAuditLog, ignoreStatuses);

        request = new MockHttpServletRequest(METHOD, REQUEST_PATH);
        request.addHeader(AuditConfiguration.REQUEST_ID, REQUEST_ID);
        response = new MockHttpServletResponse();
        response.setStatus(STATUS_NOT_HIDDEN);

        AuditContextHolder.setAuditContext(auditContext);
    }

    @Test
    @DisplayName("Should prepare audit context with HTTP semantics")
    void shouldPrepareAuditContextWithHttpSemantics() {
        // GIVEN
        given(handler.hasMethodAnnotation(LogAudit.class)).willReturn(true);

        // WHEN
        underTest.afterCompletion(request, response, handler, null);

        // THEN
        assertThat(auditContext)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getHttpStatus()).isEqualTo(response.getStatus());
                assertThat(x.getHttpMethod()).isEqualTo(METHOD);
                assertThat(x.getRequestPath()).isEqualTo(REQUEST_PATH);
                assertThat(x.getRequestId()).isEqualTo(REQUEST_ID);
            });

        verify(auditService).audit(auditContext);
    }

    @Test
    @DisplayName("Should prepare audit context with InvokingService")
    void shouldPrepareAuditContextWithInvokingService() {
        // GIVEN
        given(handler.hasMethodAnnotation(LogAudit.class)).willReturn(true);
        final String testInvokingServiceName = "Test InvokingService";
        given(auditService.getInvokingService(any())).willReturn(testInvokingServiceName);

        // WHEN
        underTest.afterCompletion(request, response, handler, null);

        // THEN
        assertThat(auditContext)
            .isNotNull()
            .satisfies(x -> assertThat(x.getInvokingService()).isEqualTo(testInvokingServiceName));

        verify(auditService).audit(auditContext);
    }

    @Test
    @DisplayName("Should prepare audit context with OperationType")
    void shouldPrepareAuditContextWithOperationType() {
        // GIVEN
        given(handler.hasMethodAnnotation(LogAudit.class)).willReturn(true);
        given(handler.getMethodAnnotation(LogAudit.class)).willReturn(logAudit);
        given(logAudit.operationType()).willReturn(AuditOperationType.DOWNLOAD_DOCUMENT_BY_ID);

        // WHEN
        underTest.afterCompletion(request, response, handler, null);

        // THEN
        assertThat(auditContext)
            .isNotNull()
            .satisfies(
                x -> assertThat(x.getAuditOperationType()).isEqualTo(AuditOperationType.DOWNLOAD_DOCUMENT_BY_ID)
            );

        verify(auditService).audit(auditContext);
    }

    @Test
    @DisplayName("Should not audit when annotation is not present")
    void shouldNotAuditForWhenAnnotationIsNotPresent() {
        // GIVEN
        given(handler.hasMethodAnnotation(LogAudit.class)).willReturn(false);

        // WHEN
        underTest.afterCompletion(request, response, handler, null);

        // THEN
        verifyNoMoreInteractions(auditService);
    }

    @Test
    @DisplayName("Should not audit when status is hidden from audit")
    void shouldNotAuditWhenStatusIsHidden() {
        // GIVEN
        response.setStatus(STATUS_HIDDEN);
        given(handler.hasMethodAnnotation(LogAudit.class)).willReturn(true);

        // WHEN
        underTest.afterCompletion(request, response, handler, null);

        // THEN
        verifyNoMoreInteractions(auditService);
    }

    @Test
    @DisplayName("Should always clear audit context")
    void shouldClearAuditContextAlways() {
        // GIVEN
        given(handler.hasMethodAnnotation(LogAudit.class)).willReturn(true);
        doThrow(new RuntimeException("audit failure")).when(auditService).audit(auditContext);

        // WHEN
        underTest.afterCompletion(request, response, handler, null);

        // THEN
        assertThat(AuditContextHolder.getAuditContext()).isNull();
    }

    @Test
    @DisplayName("Should not audit if disabled")
    void shouldNotAuditIfDisabled() {
        // GIVEN
        final boolean disableAuditLog = false;
        underTest = new AuditInterceptor(auditService, disableAuditLog, ignoreStatuses);

        // WHEN
        underTest.afterCompletion(request, response, handler, null);

        // THEN
        verifyNoMoreInteractions(auditService);
    }

    @Test
    @DisplayName("Should not audit if bad handler")
    void shouldNotAuditIfBadHandler() {
        // WHEN
        underTest.afterCompletion(request, response, new Object(), null);

        // THEN
        verifyNoMoreInteractions(auditService);
    }

}
