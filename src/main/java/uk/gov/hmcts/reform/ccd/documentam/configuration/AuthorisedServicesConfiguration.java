package uk.gov.hmcts.reform.ccd.documentam.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;

import java.io.InputStream;

@Lazy
@Slf4j
@Configuration
public class AuthorisedServicesConfiguration {

    @Bean
    @SneakyThrows
    public AuthorisedServices provideAuthorisedServices(final ObjectMapper objectMapper) {
        try (final InputStream inputStream = AuthorisedServicesConfiguration.class.getClassLoader()
            .getResourceAsStream("service_config.json")) {

            final AuthorisedServices authorisedServices = objectMapper.readValue(inputStream, AuthorisedServices.class);

            log.info("services config loaded {}", authorisedServices);

            return authorisedServices;
        }
    }

}
