package uk.gov.hmcts.reform.ccd.documentam.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.TimeZone;

/**
 * Global Jackson configuration for JSON serialization/deserialization.
 * Configures snake_case property naming and ISO-8601 date formatting across the application.
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> builder
            .changeDefaultPropertyInclusion(incl ->
                                                incl.withValueInclusion(JsonInclude.Include.NON_NULL))
            .defaultTimeZone(TimeZone.getTimeZone("UTC"))
            .defaultDateFormat(new StdDateFormat().withColonInTimeZone(true))
            .enable(DateTimeFeature.WRITE_UTC_AS_OFFSET);
    }

    @Bean
    public JsonMapper jsonMapper(JsonMapperBuilderCustomizer customizer) {
        JsonMapper.Builder builder = JsonMapper.builder();
        customizer.customize(builder);
        return builder.build();
    }

}



