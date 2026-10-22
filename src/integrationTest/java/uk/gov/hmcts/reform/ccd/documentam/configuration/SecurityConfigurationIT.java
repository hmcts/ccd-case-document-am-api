package uk.gov.hmcts.reform.ccd.documentam.configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.security.SafeServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.ccd.documentam.security.filters.ExceptionHandlingFilter;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.METADATA_JURISDICTION_ID;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentUrlWithReadPermissions;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubGetDocumentMetaData;

class SecurityConfigurationIT extends BaseTest implements TestFixture {

    private static final String CLIENT_REGISTRATION_REPOSITORY =
        "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticationFailuresKeepResponsesSafeAndRetainServerDiagnostics() throws Exception {
        assertThat(applicationContext.getBean(ServiceAuthFilter.class)).isInstanceOf(SafeServiceAuthFilter.class);
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            mockMvc.perform(get("/cases/documents/" + DOCUMENT_ID))
                .andExpect(status().isUnauthorized()).andExpect(content().string(""))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
            mockMvc.perform(get("/cases/documents/" + DOCUMENT_ID)
                    .headers(createHttpHeaders("private-disallowed-service")))
                .andExpect(status().isForbidden()).andExpect(content().string(""))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
            HttpHeaders headers = createHttpHeaders(SERVICE_NAME_XUI_WEBAPP);
            headers.remove(HttpHeaders.AUTHORIZATION);
            mockMvc.perform(get("/cases/documents/" + DOCUMENT_ID).headers(headers))
                .andExpect(status().isUnauthorized()).andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
            headers.setBearerAuth("private-invalid-token");
            mockMvc.perform(get("/cases/documents/" + DOCUMENT_ID).headers(headers))
                .andExpect(status().isUnauthorized()).andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\""));
            assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .hasSize(4).allSatisfy(event -> {
                    if (event.getFormattedMessage().startsWith("Service access denied:")) {
                        assertThat(event.getThrowableProxy()).isNull();
                    } else {
                        assertThat(event.getThrowableProxy()).isNotNull();
                    }
                    assertThat(event.getFormattedMessage()).doesNotContain(
                        DOCUMENT_ID.toString(), "private-disallowed-service", "private-invalid-token", "Exception");
                });
        } finally {
            root.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void idamFailuresRetainOriginalCausesAtInfoAndDebug() throws Exception {
        var stub = com.github.tomakehurst.wiremock.client.WireMock.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.get("/o/userinfo").atPriority(1)
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(500)
                    .withBody("private-idam-diagnostic")));
        var cache = applicationContext.getBean(org.springframework.cache.CacheManager.class).getCache("userInfoCache");
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Logger logger = (Logger) LoggerFactory.getLogger(ExceptionHandlingFilter.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            cache.clear();
            for (Level level : new Level[] {Level.INFO, Level.DEBUG}) {
                logger.setLevel(level);
                appender.list.clear();
                mockMvc.perform(get("/cases/documents/" + DOCUMENT_ID)
                        .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP)))
                    .andExpect(status().isInternalServerError()).andExpect(content().string(""))
                    .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
                assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                    .hasSize(1).allSatisfy(event -> {
                        assertThat(event.getLoggerName()).isEqualTo(logger.getName());
                        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                        assertThat(event.getThrowableProxy()).isNotNull();
                        assertThat(event.getThrowableProxy().getMessage()).contains("private-idam-diagnostic");
                        assertThat(event.getFormattedMessage()).isEqualTo(
                            "Security filter request failed: code=INTERNAL_ERROR status=500");
                    });
                assertThat(appender.list).filteredOn(event -> event.getLoggerName().equals(logger.getName()))
                    .hasSize(1);
            }
        } finally {
            logger.setLevel(previousLevel);
            root.detachAppender(appender);
            appender.stop();
            cache.clear();
            com.github.tomakehurst.wiremock.client.WireMock.removeStub(stub);
        }
    }

    @Test
    void shouldAuthenticateBearerJwtWithoutOauth2ClientRegistration() throws Exception {
        assertThat(applicationContext.getEnvironment()
                       .containsProperty("spring.security.oauth2.client.registration.oidc.client-secret"))
            .isFalse();
        assertThat(getBeanNamesForTypeIfPresent(CLIENT_REGISTRATION_REPOSITORY))
            .isEmpty();

        stubDocumentUrlWithReadPermissions();
        stubGetDocumentMetaData(buildDocument());

        mockMvc.perform(get("/cases/documents/" + DOCUMENT_ID)
                            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.metadata." + METADATA_CASE_ID, is(CASE_ID_VALUE)))
            .andExpect(jsonPath("$.metadata." + METADATA_CASE_TYPE_ID, is(CASE_TYPE_ID_VALUE)))
            .andExpect(jsonPath("$.metadata." + METADATA_JURISDICTION_ID, is(JURISDICTION_ID_VALUE)));
    }

    private String[] getBeanNamesForTypeIfPresent(String className) throws ClassNotFoundException {
        if (!ClassUtils.isPresent(className, applicationContext.getClassLoader())) {
            return new String[0];
        }

        return applicationContext.getBeanNamesForType(Class.forName(className));
    }

    private Document buildDocument() {
        return Document.builder()
            .classification(Classification.PUBLIC)
            .createdOn(new Date())
            .metadata(Map.of(
                METADATA_CASE_ID, CASE_ID_VALUE,
                METADATA_CASE_TYPE_ID, CASE_TYPE_ID_VALUE,
                METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE
            ))
            .links(TestFixture.getLinks())
            .build();
    }
}
