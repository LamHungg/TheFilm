package vn.thefilm.backend.security;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.config.BeanDefinition;

/** Spring AOP advisor enforcing {@link RequireCinemaScope} on controller methods. */
@Component
@Lazy
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class CinemaScopeAspect implements PointcutAdvisor, MethodBeforeAdvice {

    private final CinemaScopeValidator validator;
    private final Pointcut pointcut = new CinemaScopePointcut();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public CinemaScopeAspect(@Lazy CinemaScopeValidator validator) {
        this.validator = validator;
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public org.aopalliance.aop.Advice getAdvice() {
        return this;
    }

    @Override
    public boolean isPerInstance() {
        return false;
    }

    @Override
    public void before(Method method, Object[] args, Object target) {
        Method specificMethod = AopUtils.getMostSpecificMethod(method, target.getClass());
        RequireCinemaScope requirement = AnnotatedElementUtils.findMergedAnnotation(
                specificMethod, RequireCinemaScope.class);
        if (requirement == null) {
            requirement = AnnotatedElementUtils.findMergedAnnotation(method, RequireCinemaScope.class);
        }
        if (requirement == null) {
            requirement = AnnotatedElementUtils.findMergedAnnotation(
                    target.getClass(), RequireCinemaScope.class);
        }
        if (requirement == null) {
            return;
        }
        Long cinemaId = resolveCinemaId(specificMethod, args, requirement.value());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        validator.requireAccess(authentication, cinemaId);
    }

    private Long resolveCinemaId(Method method, Object[] args, String configuredName) {
        Set<Long> matches = new LinkedHashSet<>();
        boolean found = false;
        var parameters = method.getParameters();
        String[] discoveredNames = parameterNameDiscoverer.getParameterNames(method);
        for (int i = 0; i < parameters.length; i++) {
            var parameter = parameters[i];
            Object argument = args[i];
            String requestName = requestName(parameter.getAnnotation(PathVariable.class),
                    parameter.getAnnotation(RequestParam.class));
            String name = requestName.isBlank()
                    ? discoveredNames == null ? parameter.getName() : discoveredNames[i]
                    : requestName;
            if (configuredName.equals(name) && isCinemaIdScalar(argument)) {
                found = true;
                Long parameterValue = toCinemaId(argument);
                if (parameterValue == null) {
                    return null;
                }
                matches.add(parameterValue);
            } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                CinemaIdValue bodyValue = readCinemaId(argument, configuredName);
                if (bodyValue.present()) {
                    found = true;
                    if (bodyValue.value() == null) {
                        return null;
                    }
                    matches.add(bodyValue.value());
                }
            }
        }
        return found && matches.size() == 1 ? matches.iterator().next() : null;
    }

    private static String requestName(PathVariable path, RequestParam query) {
        if (path != null) {
            return firstNonBlank(path.name(), path.value());
        }
        if (query != null) {
            return firstNonBlank(query.name(), query.value());
        }
        return "";
    }

    private static CinemaIdValue readCinemaId(Object body, String configuredName) {
        if (body == null || configuredName.isBlank()) {
            return new CinemaIdValue(false, null);
        }
        if (body instanceof Map<?, ?> values) {
            if (!values.containsKey(configuredName)) {
                return new CinemaIdValue(false, null);
            }
            return new CinemaIdValue(true, toCinemaId(values.get(configuredName)));
        }
        try {
            if (body.getClass().isRecord()) {
                for (RecordComponent component : body.getClass().getRecordComponents()) {
                    if (configuredName.equals(component.getName())) {
                        Object value = component.getAccessor().invoke(body);
                        return new CinemaIdValue(true, toCinemaId(value));
                    }
                }
            }
            String getterName = "get" + Character.toUpperCase(configuredName.charAt(0)) + configuredName.substring(1);
            try {
                Object value = body.getClass().getMethod(getterName).invoke(body);
                return new CinemaIdValue(true, toCinemaId(value));
            } catch (NoSuchMethodException ignored) {
                try {
                    Method accessor = body.getClass().getMethod(configuredName);
                    Object value = accessor.invoke(body);
                    return new CinemaIdValue(true, toCinemaId(value));
                } catch (NoSuchMethodException noAccessor) {
                    for (Class<?> type = body.getClass(); type != null && type != Object.class;
                            type = type.getSuperclass()) {
                        try {
                            Field field = type.getDeclaredField(configuredName);
                            if (!field.trySetAccessible()) {
                                return new CinemaIdValue(true, null);
                            }
                            return new CinemaIdValue(true, toCinemaId(field.get(body)));
                        } catch (NoSuchFieldException notInThisType) {
                            // Check the superclass for inherited request fields.
                        }
                    }
                    return new CinemaIdValue(false, null);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return new CinemaIdValue(false, null);
        }
    }

    private record CinemaIdValue(boolean present, Long value) {
    }

    private static Long toCinemaId(Object value) {
        if (value instanceof Number number) {
            try {
                return new BigDecimal(number.toString()).longValueExact();
            } catch (NumberFormatException | ArithmeticException ignored) {
                return null;
            }
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean isCinemaIdScalar(Object value) {
        return value instanceof Number || value instanceof CharSequence;
    }

    private static String firstNonBlank(String first, String second) {
        return !first.isBlank() ? first : second;
    }

    private static final class CinemaScopePointcut implements Pointcut {
        @Override
        public ClassFilter getClassFilter() {
            return ClassFilter.TRUE;
        }

        @Override
        public MethodMatcher getMethodMatcher() {
            return new MethodMatcher() {
                @Override
                public boolean matches(Method method, Class<?> targetClass) {
                    Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
                    return AnnotatedElementUtils.hasAnnotation(specificMethod, RequireCinemaScope.class)
                            || AnnotatedElementUtils.hasAnnotation(method, RequireCinemaScope.class)
                            || AnnotatedElementUtils.hasAnnotation(targetClass, RequireCinemaScope.class);
                }

                @Override
                public boolean isRuntime() {
                    return false;
                }

                @Override
                public boolean matches(Method method, Class<?> targetClass, Object... args) {
                    return matches(method, targetClass);
                }
            };
        }
    }
}
