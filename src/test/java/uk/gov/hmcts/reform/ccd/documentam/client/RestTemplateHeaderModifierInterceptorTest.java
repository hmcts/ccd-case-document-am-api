package uk.gov.hmcts.reform.ccd.documentam.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RestTemplateHeaderModifierInterceptorTest {
    private static final String USER_ID = "1234";

    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final HttpRequest httpRequest = mock(HttpRequest.class);
    private final ClientHttpRequestExecution clientHttpRequestExecution = mock(ClientHttpRequestExecution.class);

    private final RestTemplateHeaderModifierInterceptor underTest =
        new RestTemplateHeaderModifierInterceptor(securityUtils);

    @BeforeEach
    void prepare() {
        doReturn(new HttpHeaders()).when(httpRequest).getHeaders();
        final HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.add(Constants.SERVICE_AUTHORIZATION, "service_token");
        doReturn(authHeaders).when(securityUtils).serviceAuthorizationHeaders();
        doReturn(UserInfo.builder().uid(USER_ID).build()).when(securityUtils).getUserInfo();
    }

    @Test
    void testShouldVerifyHeaderKeys() throws Exception {
        // WHEN
        underTest.intercept(httpRequest, new byte[0], clientHttpRequestExecution);

        // THEN
        assertThat(httpRequest.getHeaders())
            .isNotNull()
            .containsKeys(Constants.SERVICE_AUTHORIZATION, Constants.USERID);

        verify(httpRequest, atLeast(3)).getHeaders();
        verify(securityUtils).serviceAuthorizationHeaders();
        verify(securityUtils).getUserInfo();
    }

    @Test
    void testShouldVerifyContentType() throws Exception {
        // WHEN
        underTest.intercept(httpRequest, new byte[0], clientHttpRequestExecution);

        // THEN
        assertThat(httpRequest.getHeaders().getContentType())
            .isNotNull()
            .isEqualTo(MediaType.APPLICATION_JSON);
    }
}
