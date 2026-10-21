package uk.gov.hmcts.reform.ccd.documentam.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.authorisation.ServiceAuthAutoConfiguration;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.ccd.documentam.configuration.SafeServiceAuthConfiguration;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SafeServiceAuthFilterTest {

    private final AuthTokenValidator validator = mock(AuthTokenValidator.class);
    private final SafeServiceAuthFilter filter = new SafeServiceAuthFilter(validator, List.of("ALLOWED"));
    private final FilterChain chain = mock(FilterChain.class);

    @Test
    void preservesAuthorisationAndBearerNormalization() throws Exception {
        when(validator.getServiceName("Bearer token")).thenReturn("allowed");
        for (String token : List.of("token", "Bearer token")) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/resource");
            request.addHeader(ServiceAuthFilter.AUTHORISATION, token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
            verify(chain).doFilter(request, response);
        }
    }

    @Test
    void missingTokenRemainsAnEmpty401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(new MockHttpServletRequest(), response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
        verifyNoInteractions(validator, chain);
    }

    @Test
    void disallowedServiceRemainsAnEmpty403() throws Exception {
        when(validator.getServiceName(anyString())).thenReturn("private-service-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request(), response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
        verifyNoInteractions(chain);
    }

    @Test
    void originalDiagnosticsAreOnlyAvailableAtDebugAcrossTheLogChain() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(SafeServiceAuthFilter.class);
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        try {
            for (RuntimeException exception : List.of(new InvalidTokenException("Synthetic private diagnostic"),
                                                     new ServiceException("Synthetic private diagnostic", null))) {
                doThrow(exception).when(validator).getServiceName(anyString());
                logger.setLevel(Level.INFO);
                appender.list.clear();
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilterInternal(request(), response, chain);
                assertThat(response.getStatus()).isEqualTo(401);
                assertThat(response.getContentAsString()).isEmpty();
                assertThat(appender.list).hasSize(1);
                assertThat(appender.list.get(0).getFormattedMessage())
                    .isEqualTo("Service authentication failed: code=AUTHENTICATION_REQUIRED status=401");
                assertThat(appender.list.get(0).getThrowableProxy()).isNotNull();
                logger.setLevel(Level.DEBUG);
                appender.list.clear();
                filter.doFilterInternal(request(), new MockHttpServletResponse(), chain);
                assertThat(appender.list).hasSize(1);
                assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.WARN);
                assertThat(appender.list.get(0).getThrowableProxy().getMessage())
                    .isEqualTo("Synthetic private diagnostic");
            }
        } finally {
            logger.setLevel(previousLevel);
            rootLogger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void unexpectedDownstreamFailuresStillPropagate() throws Exception {
        when(validator.getServiceName(anyString())).thenReturn("allowed");
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(new ServletException("downstream failure")).when(chain).doFilter(request, response);
        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
            .isInstanceOf(ServletException.class);
    }

    @Test
    void replacesTheActualSharedBeanAndPreservesCustomOverrides() {
        String beanName = Arrays.stream(ServiceAuthAutoConfiguration.class.getDeclaredMethods())
            .filter(method -> method.getReturnType() == ServiceAuthFilter.class)
            .map(Method::getName).findFirst().orElseThrow();
        ServiceAuthFilter original = new ServiceAuthFilter(validator, List.of("allowed"));
        ServiceAuthFilter custom = new ServiceAuthFilter(validator, List.of("custom"));
        ServiceAuthorisationApi api = mock(ServiceAuthorisationApi.class);
        when(api.getServiceName("Bearer token")).thenReturn("allowed");
        new ApplicationContextRunner().withUserConfiguration(SafeServiceAuthConfiguration.class)
            .withPropertyValues("idam.s2s-authorised.services=ALLOWED")
            .withBean(ServiceAuthorisationApi.class, () -> api)
            .withBean(beanName, ServiceAuthFilter.class, () -> original)
            .withBean("customFilter", ServiceAuthFilter.class, () -> custom)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(beanName)).isInstanceOf(SafeServiceAuthFilter.class);
                assertThat(context.getBean("customFilter")).isSameAs(custom);
                MockHttpServletRequest request = request();
                MockHttpServletResponse response = new MockHttpServletResponse();
                context.getBean(beanName, ServiceAuthFilter.class).doFilter(request, response, chain);
                verify(api).getServiceName("Bearer token");
                verify(chain).doFilter(request, response);
            });
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private/resource");
        request.addHeader(ServiceAuthFilter.AUTHORISATION, "token");
        return request;
    }
}
