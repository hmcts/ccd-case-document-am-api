package uk.gov.hmcts.reform.ccd.document.am.configuration;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SERVICE_AUTHORIZATION;

import java.util.Enumeration;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.authorisation.generators.ServiceAuthTokenGenerator;

@Configuration
@Slf4j
public class FeignClientConfiguration {


    private transient ServiceAuthTokenGenerator tokenGenerator;

    @Autowired
    public FeignClientConfiguration(ServiceAuthTokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }


    @Bean
    public RequestInterceptor requestInterceptor(FeignHeaderConfig config) {
        return requestTemplate -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                Enumeration<String> headerNames = request.getHeaderNames();
                if (headerNames != null) {
                    while (headerNames.hasMoreElements()) {
                        String name = headerNames.nextElement();
                        String value = request.getHeader(name);
                        if (config.getHeaders().contains(name.toLowerCase(Locale.ENGLISH))) {
                            if (name.equalsIgnoreCase(SERVICE_AUTHORIZATION)) {
                                String serviceToken = tokenGenerator.generate();
                                System.out.println("Generated Service Token for " + name + "is: " + serviceToken);
                                requestTemplate.header(name, serviceToken);
                            } else {
                                requestTemplate.header(name, value);
                            }
                        }

                    }

                } else {
                    log.warn("FeignHeadConfiguration", "Failed to get request header!");
                }
            }
        };
    }
}
