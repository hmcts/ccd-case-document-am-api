package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Component
@PropertySource(value = "classpath:service_config.json")
@ConfigurationProperties
public class AuthorisedServices {

    @JsonProperty("authorisedServices")
    private List<AuthorisedService> authServices;

}
