package uk.gov.hmcts.reform.ccd.documentam.configuration;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggerContextListener;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.ccd.documentam.logging.SafeHealthDiagnosticsFilter;
import uk.gov.hmcts.reform.ccd.documentam.logging.SafeDependencyDiagnosticsFilter;

@Configuration
public class SafeHealthLoggingConfiguration {

    @Bean
    public static BeanPostProcessor safeHealthLoggingPostProcessor() {
        return new HealthLoggingPostProcessor();
    }

    static final class HealthLoggingPostProcessor
        implements BeanPostProcessor, DisposableBean, LoggerContextListener {

        private final LoggerContext context;
        private final SafeHealthDiagnosticsFilter filter = new SafeHealthDiagnosticsFilter();
        private final SafeDependencyDiagnosticsFilter dependencies = new SafeDependencyDiagnosticsFilter();

        private HealthLoggingPostProcessor() {
            this((LoggerContext) LoggerFactory.getILoggerFactory());
        }

        HealthLoggingPostProcessor(LoggerContext context) {
            this.context = context;
            filter.setContext(context);
            dependencies.setContext(context);
            installFilters();
            context.addListener(this);
        }

        private void installFilters() {
            for (var diagnosticFilter : java.util.List.of(filter, dependencies)) {
                diagnosticFilter.start();
                if (!context.getTurboFilterList().contains(diagnosticFilter)) {
                    context.addTurboFilter(diagnosticFilter);
                }
            }
        }

        @Override
        public boolean isResetResistant() {
            return true;
        }

        @Override
        public void onReset(LoggerContext loggerContext) {
            installFilters();
        }

        @Override
        public void onStart(LoggerContext loggerContext) {
            // Installed when the post-processor is created.
        }

        @Override
        public void onStop(LoggerContext loggerContext) {
            // Spring owns the registration lifecycle.
        }

        @Override
        public void onLevelChange(Logger logger, Level level) {
            // Each filter checks the current effective level.
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            if (bean instanceof HealthContributor contributor) {
                register(contributor);
            }
            return bean;
        }

        private void register(HealthContributor contributor) {
            if (contributor instanceof HealthIndicator) {
                filter.register(contributor.getClass());
                filter.register(AopUtils.getTargetClass(contributor));
            } else if (contributor instanceof CompositeHealthContributor composite) {
                composite.forEach(named -> register(named.getContributor()));
            }
        }

        @Override
        public void destroy() {
            context.removeListener(this);
            context.getTurboFilterList().remove(filter);
            context.getTurboFilterList().remove(dependencies);
            filter.stop();
            dependencies.stop();
        }
    }
}
