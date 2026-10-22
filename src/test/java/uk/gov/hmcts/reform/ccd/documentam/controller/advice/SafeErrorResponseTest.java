package uk.gov.hmcts.reform.ccd.documentam.controller.advice;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SafeErrorResponseTest {

    private static final String DIAGNOSTIC = "java.lang.SecretException at http://internal-service/private/file";
    private MockMvc mvc;
    private ProbeController controller;

    @BeforeEach
    void setUp() {
        controller = new ProbeController();
        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new CaseDocumentControllerAdvice()).build();
    }

    @Test
    void hidesUnexpectedExceptionAndCause() throws Exception {
        controller.failure = new RuntimeException(DIAGNOSTIC, new IllegalStateException(DIAGNOSTIC));
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(content().string(not(containsString(DIAGNOSTIC))))
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void hidesTypeConversionDiagnostics() throws Exception {
        mvc.perform(get("/error-probe/id/not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.path").value("/error-probe/id/{id}"))
            .andExpect(content().string(not(containsString("not-a-uuid"))))
            .andExpect(content().string(not(containsString("java.util.UUID"))))
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void hidesMalformedJsonDiagnostics() throws Exception {
        mvc.perform(post("/error-probe").contentType(MediaType.APPLICATION_JSON).content("{invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(content().string(not(containsString("JsonParseException"))))
            .andExpect(content().string(not(containsString("invalid"))))
            .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void retainsUnsupportedMediaAndMethodStatuses() throws Exception {
        mvc.perform(post("/error-probe").contentType(MediaType.TEXT_PLAIN).content("invalid"))
            .andExpect(status().isInternalServerError())
            .andExpect(header().string("Accept", containsString("application/json")))
            .andExpect(jsonPath("$.exception").doesNotExist());
        mvc.perform(get("/error-probe"))
            .andExpect(status().isInternalServerError())
            .andExpect(header().string("Allow", "POST"))
            .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void incompatibleAcceptHeadersKeepLegacyNegotiationAndServerDiagnostics() throws Exception {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Logger advice = (Logger) LoggerFactory.getLogger(CaseDocumentControllerAdvice.class);
        Level previousLevel = advice.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        var context = root.getLoggerContext();
        var dependencies = new uk.gov.hmcts.reform.ccd.documentam.logging.SafeDependencyDiagnosticsFilter();
        dependencies.setContext(context);
        dependencies.start();
        context.addTurboFilter(dependencies);
        MockMvc negotiated = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new CaseDocumentControllerAdvice())
            .addFilters(new uk.gov.hmcts.reform.ccd.documentam.security.filters.ExceptionHandlingFilter()).build();
        appender.start();
        root.addAppender(appender);
        controller.failure = new IllegalStateException(DIAGNOSTIC);
        try {
            advice.setLevel(Level.INFO);
            for (String accept : new String[] {"application/xml", "text/html"}) {
                negotiated.perform(get("/error-probe/json").accept(accept))
                    .andExpect(status().isNotAcceptable())
                    .andExpect(content().string(""));
                negotiated.perform(get("/error-probe/failure").accept(accept))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(""));
            }
            assertThat(appender.list)
                .filteredOn(event -> event.getLoggerName().equals(CaseDocumentControllerAdvice.class.getName()))
                .hasSize(4).allSatisfy(event -> {
                    assertThat(event.getLoggerName()).isEqualTo(CaseDocumentControllerAdvice.class.getName());
                    assertThat(event.getThrowableProxy()).isNotNull();
                });
        } finally {
            context.getTurboFilterList().remove(dependencies);
            dependencies.stop();
            advice.setLevel(previousLevel);
            root.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void responseStatusExceptionsRetainRequiredHeaders() throws Exception {
        controller.failure = new org.springframework.web.server.MethodNotAllowedException(
            "GET", java.util.List.of(org.springframework.http.HttpMethod.POST));
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", "POST"))
            .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    void validRequestsStillSucceed() throws Exception {
        mvc.perform(post("/error-probe").contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"ok\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.value").value("ok"));
    }

    @Test
    void hidesDownstreamDiagnosticsAndPreservesOriginalStatusMapping() throws Exception {
        for (int code : new int[] {400, 401, 413, 415, 422}) {
            controller.failure = org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.valueOf(code), DIAGNOSTIC,
                org.springframework.http.HttpHeaders.EMPTY,
                DIAGNOSTIC.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.charset.StandardCharsets.UTF_8);
            mvc.perform(get("/error-probe/failure"))
                .andExpect(status().is(code == 401 ? 401 : 500))
                .andExpect(jsonPath("$.code").value(code == 401 ? "AUTHENTICATION_REQUIRED" : "INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString(DIAGNOSTIC))))
                .andExpect(jsonPath("$.exception").doesNotExist());
        }
    }

    @Test
    void preservesLegacyUploadSizeFailureStatus() throws Exception {
        controller.failure = new org.springframework.web.multipart.MaxUploadSizeExceededException(100);
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void logsSafeCodesAndRetainsOriginalExceptionsAtInfo() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(CaseDocumentControllerAdvice.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        controller.failure = new RuntimeException(DIAGNOSTIC);
        try {
            logger.setLevel(Level.INFO);
            mvc.perform(get("/error-probe/failure")).andExpect(status().isInternalServerError());
            mvc.perform(get("/error-probe/id/not-a-uuid")).andExpect(status().isBadRequest());
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
                "Request failed: code=INTERNAL_ERROR status=500",
                "Request failed: code=INVALID_REQUEST status=400");
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getThrowableProxy()).isNotNull();
            });
            appender.list.clear();
            logger.setLevel(Level.DEBUG);
            mvc.perform(get("/error-probe/failure")).andExpect(status().isInternalServerError());
            assertThat(appender.list).hasSize(1);
            ILoggingEvent diagnostic = appender.list.get(0);
            assertThat(diagnostic.getLevel()).isEqualTo(Level.ERROR);
            assertThat(diagnostic.getFormattedMessage()).isEqualTo(
                "Request failed: code=INTERNAL_ERROR status=500");
            assertThat(diagnostic.getThrowableProxy().getMessage()).isEqualTo(DIAGNOSTIC);
        } finally {
            logger.setLevel(previousLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void returnsSafeAuthenticationCategoriesFromAdvice() throws Exception {
        controller.failure = new uk.gov.hmcts.reform.ccd.documentam.exception.UnauthorizedException(DIAGNOSTIC);
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(content().string(not(containsString(DIAGNOSTIC))));
        controller.failure = new uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException(DIAGNOSTIC);
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(content().string(not(containsString(DIAGNOSTIC))));
    }

    @Test
    void responseSerializationFailuresRetainBadRequestStatus() throws Exception {
        controller.failure = new org.springframework.http.converter.HttpMessageNotWritableException(DIAGNOSTIC);
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(content().string(not(containsString(DIAGNOSTIC))));
    }

    @RestController
    static class ProbeController {
        private RuntimeException failure;

        @GetMapping("/error-probe/failure")
        public void fail() {
            throw failure;
        }

        @GetMapping("/error-probe/id/{id}")
        public UUID identifier(@PathVariable("id") UUID id) {
            return id;
        }

        @GetMapping(value = "/error-probe/json", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, String> json() {
            return Map.of("value", "ok");
        }

        @PostMapping(value = "/error-probe", consumes = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, String> echo(@RequestBody Map<String, String> value) {
            return value;
        }
    }
}
