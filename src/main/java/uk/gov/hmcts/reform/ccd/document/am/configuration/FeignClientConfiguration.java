package uk.gov.hmcts.reform.ccd.document.am.configuration;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SERVICE_AUTHORIZATION;

import java.util.Enumeration;
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
                        System.out.println("Header Name :" + name + "     Value : " + value);
                        requestTemplate.header(name, value);
                    }
                    String serviceToken = tokenGenerator.generate();
                    System.out.println("Generated ServiceToken is : " + serviceToken);
                    requestTemplate.header(SERVICE_AUTHORIZATION, serviceToken);

                } else {
                    log.warn("FeignHeadConfiguration", "Failed to get request header!");
                }
            }
        };
    }
}
