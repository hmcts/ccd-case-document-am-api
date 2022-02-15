package uk.gov.hmcts.reform.ccd.documentam;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.USER_ID;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.SERVICE_AUTHORISATION_VALUE;

@TestConfiguration
public class TestSecurityUtilsConfiguration {

    @Bean
    @Primary
    public SecurityUtils provideSecurityUtils() {
        final SecurityUtils securityUtils = mock(SecurityUtils.class);

        final HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.add(Constants.SERVICE_AUTHORIZATION, SERVICE_AUTHORISATION_VALUE);
        doReturn(authHeaders).when(securityUtils).serviceAuthorizationHeaders();
        doReturn(UserInfo.builder().uid(USER_ID).build()).when(securityUtils).getUserInfo();

        return securityUtils;
    }
}
