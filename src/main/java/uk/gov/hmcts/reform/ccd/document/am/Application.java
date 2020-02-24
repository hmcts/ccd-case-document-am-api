package uk.gov.hmcts.reform.ccd.document.am;

import feign.Feign;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.ServiceAuthTokenGenerator;

@SpringBootApplication
@EnableTransactionManagement(proxyTargetClass = true)
@EnableCircuitBreaker
@EnableFeignClients
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ServiceAuthorisationApi generateServiceAuthorisationApi(@Value("${idam.s2s-auth.url}") final String s2sUrl) {
        return Feign.builder()
            .encoder(new JacksonEncoder())
            .contract(new SpringMvcContract())
            .target(ServiceAuthorisationApi.class, s2sUrl);
    }

    @Bean
    public ServiceAuthTokenGenerator authTokenGenerator(
        @Value("${idam.s2s-auth.totp_secret}") final String secret,
        @Value("${idam.s2s-auth.microservice}") final String microService,
        final ServiceAuthorisationApi serviceAuthorisationApi

    ) {
        return new ServiceAuthTokenGenerator(secret, microService, serviceAuthorisationApi);
    }


}
