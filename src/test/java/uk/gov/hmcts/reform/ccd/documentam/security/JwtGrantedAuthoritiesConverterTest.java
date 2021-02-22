package uk.gov.hmcts.reform.ccd.documentam.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ccd.documentam.security.JwtGrantedAuthoritiesConverter.BEARER;
import static uk.gov.hmcts.reform.ccd.documentam.security.JwtGrantedAuthoritiesConverter.TOKEN_NAME;

@ExtendWith(MockitoExtension.class)
class JwtGrantedAuthoritiesConverterTest {

    private static final String ACCESS_TOKEN = "access_token";

    @Mock
    private IdamRepository idamRepository;

    @InjectMocks
    private JwtGrantedAuthoritiesConverter converter;

    @Mock
    private Jwt jwt;

    @Test
    @DisplayName("No Claims should return empty authorities")
    void shouldReturnEmptyAuthoritiesWhenClaimNotAvailable() {
        when(jwt.containsClaim(TOKEN_NAME)).thenReturn(false);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertEquals(0, authorities.size(), "size must be empty");
    }

    @Test
    @DisplayName("Should return empty authorities when claim value is not access_token")
    void shouldReturnEmptyAuthoritiesWhenClaimValueNotEquals() {
        when(jwt.containsClaim(TOKEN_NAME)).thenReturn(true);
        when(jwt.getClaim(TOKEN_NAME)).thenReturn("Test");
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertEquals(0, authorities.size(), "size must be empty");
    }

    @Test
    @DisplayName("Should return empty authorities when roles are empty")
    void shouldReturnEmptyAuthoritiesWhenIdamReturnsNoUsers() {
        when(jwt.containsClaim(TOKEN_NAME)).thenReturn(true);
        when(jwt.getClaim(TOKEN_NAME)).thenReturn(ACCESS_TOKEN);
        when(jwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        UserInfo userInfo = mock(UserInfo.class);
        List<String> roles = new ArrayList<>();
        when(userInfo.getRoles()).thenReturn(roles);
        when(idamRepository.getUserInfo(BEARER + ACCESS_TOKEN)).thenReturn(userInfo);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertEquals(0, authorities.size(), "size must be empty");
    }

    @Test
    @DisplayName("Should return authorities as per roles")
    void shouldReturnAuthoritiesWhenIdamReturnsUserRoles() {
        when(jwt.containsClaim(TOKEN_NAME)).thenReturn(true);
        when(jwt.getClaim(TOKEN_NAME)).thenReturn(ACCESS_TOKEN);
        when(jwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        UserInfo userInfo = mock(UserInfo.class);
        List<String> roles = new ArrayList<>();
        roles.add("citizen");
        when(userInfo.getRoles()).thenReturn(roles);
        when(idamRepository.getUserInfo(BEARER + ACCESS_TOKEN)).thenReturn(userInfo);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertEquals(1, authorities.size(), "should return one authority");
    }
}
