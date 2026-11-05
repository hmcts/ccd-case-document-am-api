package uk.gov.hmcts.reform.ccd.documentam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import java.io.IOException;
import java.util.List;

/** Retains the shared filter's validation and status semantics with controlled logging. */
@Slf4j
public class SafeServiceAuthFilter extends ServiceAuthFilter {

    private final AuthTokenValidator validator;
    private final List<String> authorisedServices;

    public SafeServiceAuthFilter(AuthTokenValidator validator, List<String> authorisedServices) {
        super(validator, authorisedServices);
        this.validator = validator;
        this.authorisedServices = authorisedServices.stream().map(String::toLowerCase).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        try {
            String token = request.getHeader(AUTHORISATION);
            if (token == null) {
                throw new InvalidTokenException("ServiceAuthorization Token is missing");
            }
            String serviceName = validator.getServiceName(token.startsWith("Bearer ") ? token : "Bearer " + token);
            if (authorisedServices.contains(serviceName)) {
                chain.doFilter(request, response);
            } else {
                log.warn("Service access denied: code={} status=403 service={}", ErrorCode.ACCESS_DENIED, serviceName);
                response.setStatus(HttpStatus.FORBIDDEN.value());
            }
        } catch (InvalidTokenException | ServiceException exception) {
            log.warn("Service authentication failed: code={} status=401", ErrorCode.AUTHENTICATION_REQUIRED, exception);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
