package uk.gov.hmcts.reform.ccd.documentam.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.util.StdDateFormat;

import java.util.TimeZone;

/**
 * Global Jackson configuration for JSON serialization/deserialization.
 * Configures ISO-8601 date formatting across the application.
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

}



