package uk.gov.hmcts.reform.ccd.documentam.auditlog.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditDiagnosticRedactionTest {

    @Test
    void expressionFailuresAreGenericAtWarnAndDetailedOnlyAtDebug() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(SampleOperation.class.getDeclaredMethod("operation", String.class));
        when(joinPoint.getThis()).thenReturn(new SampleOperation());
        when(joinPoint.getArgs()).thenReturn(new Object[] {"synthetic-private-id"});
        Logger logger = (Logger) LoggerFactory.getLogger(AuditAspect.class);
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        try {
            logger.setLevel(Level.INFO);
            AuditAspect aspect = new AuditAspect();
            assertThat((Object) ReflectionTestUtils.invokeMethod(aspect, "getValue", joinPoint,
                "#p0.internalDiagnosticField", null, String.class)).isNull();
            assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .allSatisfy(event -> {
                    assertThat(event.getFormattedMessage())
                        .doesNotContain("internalDiagnosticField", "synthetic-private-id");
                    assertThat(event.getThrowableProxy()).isNull();
                });
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .contains("Audit condition evaluation failed");
            logger.setLevel(Level.DEBUG);
            appender.list.clear();
            ReflectionTestUtils.invokeMethod(aspect, "getValue", joinPoint,
                "#p0.internalDiagnosticField", null, String.class);
            assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.DEBUG)
                .anySatisfy(event -> assertThat(event.getThrowableProxy()).isNotNull());
        } finally {
            logger.setLevel(previousLevel);
            rootLogger.detachAppender(appender);
            appender.stop();
        }
    }

    static class SampleOperation {
        public String operation(String value) {
            return value;
        }
    }
}
