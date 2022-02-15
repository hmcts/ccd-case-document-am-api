package uk.gov.hmcts.reform.ccd.documentam.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.TestSecurityUtilsConfiguration;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.USER_ID;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.SERVICE_AUTHORISATION_VALUE;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubGetGreeting;

@ContextConfiguration(classes = {TestSecurityUtilsConfiguration.class})
public class RestTemplateHeaderModifierInterceptorIT extends BaseTest {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${wiremock.server.port}")
    private int wiremockPort;

    @Test
    void testShouldAddRequiredHttpRequestHeaders() {
        // GIVEN
        stubGetGreeting();

        // WHEN
        final String response = restTemplate.getForObject(String.format("http://localhost:%d/greeting", wiremockPort),
                                                          String.class);

        // THEN
        assertThat(response)
            .isNotNull()
            .isEqualTo("Hello World!");

        WireMock.verify(getRequestedFor(urlEqualTo("/greeting"))
                            .withHeader("Content-Type", equalTo(APPLICATION_JSON_VALUE))
                            .withHeader(Constants.SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                            .withHeader(Constants.USERID, equalTo(USER_ID))
        );
    }
}
