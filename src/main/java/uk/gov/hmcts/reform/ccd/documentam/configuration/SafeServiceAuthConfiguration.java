package uk.gov.hmcts.reform.ccd.documentam.configuration;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;
import uk.gov.hmcts.reform.ccd.documentam.security.SafeServiceAuthFilter;

@Configuration
public class SafeServiceAuthConfiguration {

    @Bean
    public static BeanPostProcessor safeServiceAuthFilterPostProcessor(
        ObjectProvider<ServiceAuthorisationApi> api, Environment environment) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                // Replace only the shared auto-configuration bean; keep application/test overrides intact.
                if ("serviceAuthFiler".equals(beanName) && bean instanceof ServiceAuthFilter) {
                    List<String> services = Binder.get(environment)
                        .bind("idam.s2s-authorised.services", Bindable.listOf(String.class)).get();
                    SafeServiceAuthFilter filter = new SafeServiceAuthFilter(
                        new ServiceAuthTokenValidator(api.getObject()), services);
                    filter.setBeanName(beanName);
                    return filter;
                }
                return bean;
            }
        };
    }
}
