package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.reform.ccd.documentam.configuration.AuthServicesJsonPropertySourceFactory;

import java.util.List;

@Component
@Data
@Validated
@PropertySource(value = "classpath:service_config.json",
    factory  = AuthServicesJsonPropertySourceFactory.class)
@ConfigurationProperties
public class AuthorisedServices {

    @Valid
    @JsonProperty("authorisedServices")
    private List<AuthorisedService> authServices;

}
