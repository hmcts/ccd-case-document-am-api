package uk.gov.hmcts.reform.ccd.documentam.security;

import com.google.common.collect.Lists;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DisplayName("SecurityUtils")
@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    private static final String SERVICE_JWT = "7gf364fg367f67";
    private static final String USER_ID = "123";
    private static final String USER_JWT = "8gf364fg367f67";

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private IdamRepository idamRepository;

    @Mock
    private AuthTokenGenerator serviceTokenGenerator;

    @InjectMocks
    private SecurityUtils securityUtils;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue(USER_JWT)
            .claim("aClaim", "aClaim")
            .claim("aud", Lists.newArrayList("ccd_gateway"))
            .header("aHeader", "aHeader")
            .build();

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("authorizationHeaders")
    void authorizationHeaders() {

        when(serviceTokenGenerator.generate()).thenReturn(SERVICE_JWT);
        doReturn(jwt).when(authentication).getPrincipal();
        doReturn(authentication).when(securityContext).getAuthentication();

        HttpHeaders headers = securityUtils.authorizationHeaders();

        assertAll(
            () -> assertHeader(headers, "ServiceAuthorization", SERVICE_JWT),
            () -> assertHeader(headers, "Authorization", "Bearer " + USER_JWT)
        );
    }

    @Test
    @DisplayName("serviceAuthorizationHeaders")
    void serviceAuthorizationHeaders() {

        when(serviceTokenGenerator.generate()).thenReturn(SERVICE_JWT);

        HttpHeaders headers = securityUtils.serviceAuthorizationHeaders();

        assertAll(
            () -> assertHeader(headers, "ServiceAuthorization", SERVICE_JWT)
        );
    }

    @Test
    @DisplayName("Get userInfo")
    void shouldReturnUserInfo() {
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID)
            .sub("emailId@a.com")
            .build();

        when(idamRepository.getUserInfo("Bearer " + USER_JWT)).thenReturn(userInfo);

        doReturn(jwt).when(authentication).getPrincipal();
        doReturn(authentication).when(securityContext).getAuthentication();

        assertThat(securityUtils.getUserInfo(), is(userInfo));
    }

    @Test
    @DisplayName("Get user token")
    void shouldReturnUserToken() {

        doReturn(jwt).when(authentication).getPrincipal();
        doReturn(authentication).when(securityContext).getAuthentication();

        assertThat(securityUtils.getUserToken(), is(USER_JWT));
    }

    @Test
    @DisplayName("Get service name from token supplied with bearer")
    void getServiceNameFromS2SToken_shouldReturnNameFromTokenWithBearer() {
        // ARRANGE
        String serviceName = "my-service";
        String s2STokenWithBearer = "Bearer " + generateDummyS2SToken(serviceName);

        // ACT
        String result = securityUtils.getServiceNameFromS2SToken(s2STokenWithBearer);

        // ASSERT
        assertThat(result, is(serviceName));
    }

    @Test
    @DisplayName("Get service name from token supplied without bearer")
    void getServiceNameFromS2SToken_shouldReturnNameFromTokenWithoutBearer() {
        // ARRANGE
        String serviceName = "my-service";
        String s2SToken = generateDummyS2SToken(serviceName);

        // ACT
        String result = securityUtils.getServiceNameFromS2SToken(s2SToken);

        // ASSERT
        assertThat(result, is(serviceName));
    }

    private void assertHeader(HttpHeaders headers, String name, String value) {
        assertThat(headers.get(name), hasSize(1));
        assertThat(headers.get(name).get(0), equalTo(value));
    }

    private GrantedAuthority newAuthority(String authority) {
        return (GrantedAuthority) () -> authority;
    }

    private static String generateDummyS2SToken(String serviceName) {
        return Jwts.builder()
            .setSubject(serviceName)
            .setIssuedAt(new Date())
            .signWith(SignatureAlgorithm.HS256, TextCodec.BASE64.encode("AA"))
            .compact();
    }
}
