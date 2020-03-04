package uk.gov.hmcts.reform.ccd.document.am.util;

import java.util.stream.Collectors;

import feign.Feign;
import feign.jackson.JacksonEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.ServiceAuthTokenGenerator;

@Service
@Slf4j
public class SecurityUtils {

    //@Value("${idam.s2s-auth.totp_secret}")
    private static String secret = "L5VAT7MQHB67FHB4";
    //@Value("${idam.s2s-auth.microservice}")
    private static String microService = "ccd_case_document_am_api";
    @Value("${idam.s2s-auth.url}")
    private static String s2sUrl = "http://rpe-service-auth-provider-aat.service.core-compute-aat.internal";

    @Qualifier("serviceAuthTokenGenerator")
    private transient ServiceAuthTokenGenerator serviceAuthTokenGeneratorAutowired;

    private ServiceAuthTokenGenerator getServiceAuthTokenGenerator() {
        System.out.println("Microservice: " + microService);
        System.out.println("Microservice: " + secret);
        System.out.println("Microservice: " + s2sUrl);
        ServiceAuthorisationApi serviceAuthorisationApi = Feign.builder()
                                                               .encoder(new JacksonEncoder())
                                                               .contract(new SpringMvcContract())
                                                               .target(ServiceAuthorisationApi.class, s2sUrl);
        return new ServiceAuthTokenGenerator(secret, microService, serviceAuthorisationApi);
    }

    public MultiValueMap<String, String> authorizationHeaders() {
        log.error("Generating the service Token");
        String serviceAuthToken = "value";
        try {
            log.error("Generating the service Token inside try method");
            log.error("Generating the service Token by properties file");
            getServiceAuthTokenGenerator().generate();
            log.error("Generating the service Token by autowired bean");
            //serviceAuthTokenGeneratorAutowired.generate();
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        log.error("Generated the service token : " + serviceAuthToken);
        final HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", getServiceAuthTokenGenerator().generate());
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
