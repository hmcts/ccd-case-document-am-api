package uk.gov.hmcts.reform.ccd.documentam.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceAuthenticationDiagnosticsTest {

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "unavailable", "denied", "allowed"})
    void scopesFailureMetadataWithoutChangingResponsesOrDownstreamContext(String outcome) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(SafeServiceAuthFilter.class);
        Level previousLevel = logger.getLevel();
        var previousContext = MDC.getCopyOfContextMap();
        var events = new ListAppender<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        events.start();
        logger.addAppender(events);
        try {
            logger.setLevel(Level.INFO);
            MDC.clear();
            MDC.put("operationId", "test-operation");
            MDC.put("failureCode", "outer-code");
            MDC.put("httpStatus", "outer-status");
            var validator = mock(AuthTokenValidator.class);
            if (outcome.equals("invalid")) {
                when(validator.getServiceName(anyString())).thenThrow(new InvalidTokenException("PRIVATE cause"));
            } else if (outcome.equals("unavailable")) {
                when(validator.getServiceName(anyString())).thenThrow(new ServiceException("PRIVATE cause", null));
            } else {
                when(validator.getServiceName(anyString())).thenReturn(outcome.equals("denied") ? "denied" : "allowed");
            }
            var request = new MockHttpServletRequest();
            if (!outcome.equals("missing")) {
                request.addHeader("ServiceAuthorization", "token");
            }
            var response = new MockHttpServletResponse();
            var calls = new AtomicInteger();
            FilterChain chain = (req, res) -> {
                calls.incrementAndGet();
                assertThat(MDC.get("failureCode")).isEqualTo("outer-code");
                assertThat(MDC.get("httpStatus")).isEqualTo("outer-status");
                assertThat(MDC.get("operationId")).isEqualTo("test-operation");
            };
            new SafeServiceAuthFilter(validator, List.of("allowed")).doFilter(request, response, chain);
            int status = outcome.equals("allowed") ? 200 : outcome.equals("denied") ? 403 : 401;
            assertThat(response.getStatus()).isEqualTo(status);
            assertThat(response.getContentAsString()).isEmpty();
            assertThat(response.getHeader("WWW-Authenticate")).isNull();
            assertThat(calls.get()).isEqualTo(status == 200 ? 1 : 0);
            if (status == 200) {
                assertThat(events.list).isEmpty();
            } else {
                assertThat(events.list).singleElement().satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getMDCPropertyMap())
                        .containsEntry("failureCode", status == 403 ? "AUTHORISATION_DENIED" : "AUTHENTICATION_FAILED")
                        .containsEntry("httpStatus", Integer.toString(status))
                        .containsEntry("operationId", "test-operation");
                    if (status == 401) {
                        assertThat(event.getThrowableProxy()).isNotNull();
                        assertThat(event.getThrowableProxy().getStackTraceElementProxyArray()).isNotEmpty();
                    }
                });
            }
            assertThat(MDC.get("failureCode")).isEqualTo("outer-code");
            assertThat(MDC.get("httpStatus")).isEqualTo("outer-status");
            assertThat(MDC.get("operationId")).isEqualTo("test-operation");
        } finally {
            logger.detachAppender(events);
            events.stop();
            logger.setLevel(previousLevel);
            MDC.clear();
            if (previousContext != null) {
                MDC.setContextMap(previousContext);
            }
        }
    }
}
