package uk.gov.hmcts.reform.ccd.document.am.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SERVICE_AUTHORIZATION;

@Service
public class SecurityUtils {
    private final AuthTokenGenerator authTokenGenerator;

    @Autowired
    public SecurityUtils(final AuthTokenGenerator authTokenGenerator) {
        this.authTokenGenerator = authTokenGenerator;
    }

    public HttpHeaders authorizationHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(SERVICE_AUTHORIZATION, authTokenGenerator.generate());
        headers.add("user-id", getUserId());
        headers.add("user-roles", getUserRolesHeader());
        return headers;
    }


    public String getUserId() {
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return serviceAndUser.getUsername();
    }



    public String getUserRolesHeader() {
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return serviceAndUser.getAuthorities()
                             .stream()
                             .map(GrantedAuthority::getAuthority)
                             .collect(Collectors.joining(","));
    }
}
