package uk.gov.hmcts.reform.ccd.documentam.security.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class ExceptionHandlingFilterTest {

    private MockHttpServletRequest request;

    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ExceptionHandlingFilter filter;

    private Logger logger;
    private Level previousLevel;
    private ListAppender<ILoggingEvent> listAppender;

    private static final String EXCEPTION_MESSAGE = "Security filter request failed: code=INTERNAL_ERROR status=";
    private static final String EXCEPTION_LOGGER_TYPE =
        "uk.gov.hmcts.reform.ccd.documentam.security.filters.ExceptionHandlingFilter";

    @BeforeEach
    public void setUp() {
        filter = new ExceptionHandlingFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        logger = (Logger) LoggerFactory.getLogger(ExceptionHandlingFilter.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        // create and start a ListAppender
        listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        logger.addAppender(listAppender);
    }

    @AfterEach
    public void tearDown() {
        logger.setLevel(previousLevel);
        listAppender.stop();
        logger.detachAppender(listAppender);
    }

    @Test
    public void shouldReturn502ResponseWhenClientAbortExceptionThrown() throws ServletException, IOException {
        Mockito.doThrow(ClientAbortException.class)
            .when(filterChain)
            .doFilter(Mockito.eq(request), Mockito.eq(response));
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(502);
        assertLogging();
    }

    @Test
    public void shouldReturn500ResponseWhenAnyExceptionExceptClientAbortExceptionThrown()
        throws ServletException, IOException {
        Mockito.doThrow(NullPointerException.class)
            .when(filterChain)
            .doFilter(Mockito.eq(request), Mockito.eq(response));
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(500);
        assertLogging();
    }

    @Test
    public void clearsUncommittedDiagnosticsOnFailure() throws ServletException, IOException {
        Mockito.doAnswer(invocation -> {
            response.setContentType("text/plain");
            response.setContentLength(100);
            response.getWriter().write("private diagnostic");
            throw new ServletException("private diagnostic");
        }).when(filterChain).doFilter(request, response);
        filter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(((MockHttpServletResponse) response).getContentAsString()).isEmpty();
        assertThat(response.getHeader("Content-Length")).isNull();
        assertLogging();
    }

    @Test
    public void doesNotRewriteAnAlreadyCommittedResponse() throws ServletException, IOException {
        Mockito.doAnswer(invocation -> {
            response.setStatus(206);
            response.getWriter().write("partial content");
            response.flushBuffer();
            throw new IOException("private diagnostic");
        }).when(filterChain).doFilter(request, response);
        filter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(206);
        assertThat(((MockHttpServletResponse) response).getContentAsString()).isEqualTo("partial content");
        assertLogging();
    }

    @Test
    public void originalFailureIsAvailableOnlyAtDebug() throws ServletException, IOException {
        logger.setLevel(Level.DEBUG);
        Mockito.doThrow(new ServletException("Synthetic filter diagnostic"))
            .when(filterChain).doFilter(request, response);
        filter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(listAppender.list.size()).isEqualTo(2);
        assertThat(listAppender.list.get(0).getThrowableProxy()).isNull();
        assertThat(listAppender.list.get(1).getLevel()).isEqualTo(Level.DEBUG);
        assertThat(listAppender.list.get(1).getThrowableProxy().getMessage()).isEqualTo("Synthetic filter diagnostic");
    }

    private void assertLogging() {
        List<ILoggingEvent> logsList = listAppender.list;
        ILoggingEvent lastLogEntry = logsList.get(logsList.size() - 1);

        assertThat(lastLogEntry.getLevel()).isEqualTo(Level.ERROR);
        assertThat(lastLogEntry.getLoggerName()).isEqualTo(EXCEPTION_LOGGER_TYPE);
        assertThat(lastLogEntry.getFormattedMessage()).contains(EXCEPTION_MESSAGE);
        assertThat(lastLogEntry.getThrowableProxy()).isNull();
    }
}
