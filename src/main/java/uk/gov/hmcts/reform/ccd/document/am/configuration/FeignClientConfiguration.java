package uk.gov.hmcts.reform.ccd.document.am.configuration;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SERVICE_AUTHORIZATION;

import java.util.Enumeration;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.authorisation.generators.ServiceAuthTokenGenerator;

@Configuration
@Slf4j
public class FeignClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(FeignClientConfiguration.class);

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
                    System.out.println(" headerNames is : " + headerNames);
                    LOG.error("headerNames is : " + headerNames);
                    while (headerNames.hasMoreElements()) {
                        String name = headerNames.nextElement();
                        String value = request.getHeader(name);
                        System.out.println("Header Name :" + name + "     Value : " + value);
                        if (config.getHeaders().contains(name.toLowerCase(Locale.ENGLISH))) {
                            System.out.println("Config headers has the name " + name);
                            if (name.equals(SERVICE_AUTHORIZATION)) {
                                requestTemplate.header(name, tokenGenerator.generate());
                            } else {
                                requestTemplate.header(name, value);
                            }
                        }

                    }
                    /*String serviceToken = tokenGenerator.generate();
                    System.out.println("Generated ServiceToken is : " + serviceToken);
                    LOG.error("Generated ServiceToken is : " + serviceToken);
                    requestTemplate.header(SERVICE_AUTHORIZATION, serviceToken);*/

                } else {
                    log.warn("FeignHeadConfiguration", "Failed to get request header!");
                }
            }
        };
    }
}
