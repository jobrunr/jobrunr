package org.jobrunr.utils.reflection;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;

public class MethodFinderPredicate implements Predicate<Method> {

    private final String methodName;
    private final Class<?>[] parameterTypes;

    public MethodFinderPredicate(String methodName, Class<?>... parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    @Override
    public boolean test(Method method) {
        return methodName.equals(method.getName()) && (
                Arrays.equals(method.getParameterTypes(), parameterTypes)
                        || compareParameterTypesForPrimitives(method.getParameterTypes()));
    }

    private boolean compareParameterTypesForPrimitives(Class<?>[] parameterTypes) {
        if (this.parameterTypes.length != parameterTypes.length) return false;

        boolean result = true;
        for (int i = 0; i < parameterTypes.length; i++) {
            result &= ReflectionUtils.isClassAssignable(parameterTypes[i], this.parameterTypes[i]);
        }
        return result;
    }
}
