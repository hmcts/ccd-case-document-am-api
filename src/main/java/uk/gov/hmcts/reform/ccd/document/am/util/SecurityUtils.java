package uk.gov.hmcts.reform.ccd.document.am.util;

import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;
import uk.gov.hmcts.reform.authorisation.generators.ServiceAuthTokenGenerator;

@Service
@Slf4j
public class SecurityUtils {
    private transient ServiceAuthTokenGenerator serviceAuthTokenGenerator;

    @Autowired
    public SecurityUtils(final ServiceAuthTokenGenerator serviceAuthTokenGenerator) {
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
    }

    public MultiValueMap<String, String> authorizationHeaders() {
        log.error("Generating the service Token");
        String serviceAuthToken = serviceAuthTokenGenerator.generate();
        log.error("Generated the service token : " + serviceAuthToken);
        final HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", serviceAuthToken);
        // headers.add("user-id", getUserId());
        headers.add("user-roles", "caseworker");
        log.error("Headers: " + headers.keySet());
        for (String key : headers.keySet()) {
            log.error("Key : " + key);
            log.error("value : " + headers.get(key));
        }
        /*  if (SecurityContextHolder.getContext().getAuthentication() != null) {
            final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (serviceAndUser.getPassword() != null) {
                headers.add(HttpHeaders.AUTHORIZATION, serviceAndUser.getPassword());
            }
        }*/
        return headers;
    }

    public HttpHeaders userAuthorizationHeaders() {
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, serviceAndUser.getPassword());
        return headers;
    }

    public String getUserId() {
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return serviceAndUser.getUsername();
    }

    public String getUserToken() {
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return serviceAndUser.getPassword();
    }

    public String getUserRolesHeader() {
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return serviceAndUser.getAuthorities()
                             .stream()
                             .map(GrantedAuthority::getAuthority)
                             .collect(Collectors.joining(","));
    }
}
