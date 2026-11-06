package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;

import java.io.IOException;
import java.util.Map;

public class AuthServicesJsonPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        AuthorisedServices authServices = JsonMapper.builder().build().readValue(resource.getInputStream(),
                                                                       AuthorisedServices.class);
        return new MapPropertySource("json-source", Map.of("authServices", authServices.getAuthServices()));
    }

}
