package uk.gov.hmcts.reform.ccd.documentam.auditlog.aop;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Utility class handling the SpEL expression parsing.
 *
 */
public class ExpressionEvaluator extends CachedExpressionEvaluator {

    // shared param discoverer since it caches data internally
    private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);

    private final Map<AnnotatedElementKey, Method> targetMethodCache = new ConcurrentHashMap<>(64);

    public EvaluationContext createEvaluationContext(Object object,
                                                     Class<?> targetClass,
                                                     Method method, Object[] args) {
        var targetMethod = getTargetMethod(targetClass, method);
        var root = new ExpressionRootObject(object, args);
        return new MethodBasedEvaluationContext(root, targetMethod, args, this.paramNameDiscoverer);
    }

    public <T> T condition(String conditionExpression,
                           AnnotatedElementKey elementKey,
                           EvaluationContext evalContext,
                           Class<T> clazz) {
        return getExpression(this.conditionCache, elementKey, conditionExpression).getValue(evalContext, clazz);
    }

    private Method getTargetMethod(Class<?> targetClass, Method method) {
        var methodKey = new AnnotatedElementKey(method, targetClass);

        return this.targetMethodCache
            .computeIfAbsent(methodKey, k -> AopUtils.getMostSpecificMethod(method, targetClass));
    }

    @AllArgsConstructor
    @Getter
    private static class ExpressionRootObject {
        private final Object object;
        private final Object[] args;
    }

}

