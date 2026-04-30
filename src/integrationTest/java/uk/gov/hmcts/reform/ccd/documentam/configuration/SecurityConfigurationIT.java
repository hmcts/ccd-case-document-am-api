package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
