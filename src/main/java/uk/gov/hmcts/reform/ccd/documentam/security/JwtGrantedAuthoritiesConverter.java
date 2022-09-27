package uk.gov.hmcts.reform.ccd.documentam.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;

@Component
public class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static final String TOKEN_NAME = "tokenName";
    public static final String BEARER = "Bearer ";

    private final IdamRepository idamRepository;

    @Autowired
    public JwtGrantedAuthoritiesConverter(IdamRepository idamRepository) {
        this.idamRepository = idamRepository;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        if (jwt.hasClaim(TOKEN_NAME) && jwt.getClaim(TOKEN_NAME).equals(ACCESS_TOKEN)) {
            UserInfo userInfo = idamRepository.getUserInfo(BEARER + jwt.getTokenValue());
            return extractAuthorityFromClaims(userInfo.getRoles());
        }
        return Collections.emptyList();
    }

    private List<GrantedAuthority> extractAuthorityFromClaims(List<String> roles) {
        return roles == null ? Collections.emptyList() : roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

}
