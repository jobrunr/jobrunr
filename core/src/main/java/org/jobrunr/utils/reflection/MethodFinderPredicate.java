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

        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].isPrimitive()) {
                if (!ReflectionUtils.isClassAssignable(parameterTypes[i], this.parameterTypes[i])) {
                    return false;
                }
            } else {
                if (!parameterTypes[i].equals(this.parameterTypes[i])) {
                    return false;
                }
            }
        }
        return true;
    }
}
