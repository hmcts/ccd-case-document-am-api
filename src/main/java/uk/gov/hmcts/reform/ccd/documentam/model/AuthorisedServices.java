package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.documentam.configuration.AuthServicesJsonPropertySourceFactory;

import java.util.List;

@Component
@Data
@PropertySource(value = "classpath:service_config.json",
    factory  = AuthServicesJsonPropertySourceFactory.class)
@ConfigurationProperties
public class AuthorisedServices {

    @JsonProperty("authorisedServices")
    private List<AuthorisedService> authServices;

    // A system read role must be limited to the service's own jurisdiction, never every jurisdiction
    @PostConstruct
    void validate() {
        authServices.stream()
            .filter(service -> service.getSystemReadRole() != null && "*".equals(service.getJurisdictionId()))
            .findFirst()
            .ifPresent(service -> {
                throw new IllegalStateException("Service " + service.getId()
                    + " cannot have a systemReadRole with a wildcard jurisdiction");
            });
    }
}
