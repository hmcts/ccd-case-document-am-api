package uk.gov.hmcts.reform.ccd.documentam.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;

@Component
public class SecurityUtils {

    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    public SecurityUtils(final AuthTokenGenerator authTokenGenerator) {
        this.authTokenGenerator = authTokenGenerator;
    }

    public HttpHeaders authorizationHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(Constants.SERVICE_AUTHORIZATION, authTokenGenerator.generate());

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext()
                                                                                                      .getAuthentication()
                                                                                                      .getPrincipal();
            if (serviceAndUser.getPassword() != null) {
                headers.add(HttpHeaders.AUTHORIZATION, serviceAndUser.getPassword());
            }
        }
        return headers;
    }

    public HttpHeaders serviceAuthorizationHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(Constants.SERVICE_AUTHORIZATION, authTokenGenerator.generate());
        return headers;
    }

    public String getUserId() {
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext()
                                                                                                  .getAuthentication()
                                                                                                  .getPrincipal();
        return serviceAndUser.getUsername();
    }

    public String getServiceId() {
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext()
                                                                                                  .getAuthentication()
                                                                                                  .getPrincipal();
        return serviceAndUser.getServicename();
    }
}
