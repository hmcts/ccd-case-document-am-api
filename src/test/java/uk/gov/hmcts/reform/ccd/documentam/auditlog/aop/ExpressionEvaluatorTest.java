package uk.gov.hmcts.reform.ccd.documentam.auditlog.aop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.util.ReflectionUtils;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

class ExpressionEvaluatorTest implements TestFixture {

    private final ExpressionEvaluator underTest = new ExpressionEvaluator();

    @Test
    @DisplayName("should create valuation context")
    void shouldCreateEvaluationContext() {

        // GIVEN
        final Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", String.class, Boolean.class);

        // WHEN
        final EvaluationContext context = underTest.createEvaluationContext(
            this,
            SampleMethods.class,
            method,
            new Object[]{TEST_STRING, RANDOM_INT}
        );

        // THEN
        assertThat(context)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.lookupVariable("a0")).isEqualTo(TEST_STRING);
                assertThat(x.lookupVariable("p0")).isEqualTo(TEST_STRING);
                assertThat(x.lookupVariable("foo")).isEqualTo(TEST_STRING);

                assertThat(x.lookupVariable("p1")).isEqualTo(RANDOM_INT);
                assertThat(x.lookupVariable("a1")).isEqualTo(RANDOM_INT);
                assertThat(x.lookupVariable("flag")).isEqualTo(RANDOM_INT);

                assertThat(x.lookupVariable("p2")).isNull();
                assertThat(x.lookupVariable("a2")).isNull();
            });
    }

    @Test
    @DisplayName("should parse bean expression")
    void shouldParseBeanExpressions() {

        // GIVEN
        final Tuple3<SampleBean, EvaluationContext, AnnotatedElementKey> tuple = preamble();
        final SampleBean sampleBean = tuple.getT1();
        final EvaluationContext context = tuple.getT2();
        final AnnotatedElementKey elementKey = tuple.getT3();

        // WHEN
        final UUID resultUuid = underTest.condition(
            "#sampleBean.fieldA",
            elementKey,
            context,
            UUID.class
        );
        final long resultLong = underTest.condition(
            "#sampleBean.fieldB",
            elementKey,
            context,
            long.class
        );
        final String resultString = underTest.condition(
            "#sampleBean.fieldC",
            elementKey,
            context,
            String.class
        );

        // THEN
        assertThat(resultUuid).isEqualTo(sampleBean.fieldA);
        assertThat(resultLong).isEqualTo(sampleBean.fieldB);
        assertThat(resultString).isEqualTo(sampleBean.fieldC);
    }

    @Test
    @DisplayName("should throw error when property not found")
    void shouldThrowErrorWhenPropertyNotFound() {

        // GIVEN
        final Tuple3<SampleBean, EvaluationContext, AnnotatedElementKey> tuple = preamble();
        final EvaluationContext context = tuple.getT2();
        final AnnotatedElementKey elementKey = tuple.getT3();

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.condition(
            "#sampleBean.unknownProperty",
            elementKey,
            context,
            String.class
        ));

        // THEN
        assertThat(thrown)
            .isInstanceOf(SpelEvaluationException.class)
            .hasMessageContaining("EL1008E: Property or field 'unknownProperty' cannot be found");
    }

    private Tuple3<SampleBean, EvaluationContext, AnnotatedElementKey> preamble() {
        final SampleBean sampleBean = new SampleBean(RANDOM_UUID, RANDOM_LONG, RANDOM_STRING);

        final Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", String.class, SampleBean.class);

        final EvaluationContext context = underTest.createEvaluationContext(
            this,
            SampleMethods.class,
            method,
            new Object[]{TEST_STRING, sampleBean}
        );
        final AnnotatedElementKey elementKey = new AnnotatedElementKey(
            Objects.requireNonNull(method),
            SampleMethods.class
        );

        return Tuples.of(sampleBean, context, elementKey);
    }

    @SuppressWarnings("unused")
    private static class SampleMethods {

        private void hello(String foo, Boolean flag) {
        }

        private void hello(String foo, SampleBean sampleBean) {
        }
    }

    // @Data
    // @AllArgsConstructor
    @SuppressWarnings("unused")
    private static class SampleBean {
        private final UUID fieldA;
        private final long fieldB;
        private final String fieldC;

        public SampleBean(UUID fieldA, long fieldB, String fieldC) {
            this.fieldA = fieldA;
            this.fieldB = fieldB;
            this.fieldC = fieldC;
        }

        public UUID getFieldA() {
            return fieldA;
        }

        public long getFieldB() {
            return fieldB;
        }

        public String getFieldC() {
            return fieldC;
        }
    }

}
