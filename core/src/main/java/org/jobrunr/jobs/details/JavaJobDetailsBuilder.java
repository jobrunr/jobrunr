package org.jobrunr.jobs.details;

import org.jobrunr.jobs.lambdas.IocJobLambda;

import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptorAsArray;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class JavaJobDetailsBuilder extends JobDetailsBuilder {

    public JavaJobDetailsBuilder(SerializedLambda serializedLambda, Object... params) {
        super(initLocalVariables(serializedLambda, params), toFQClassName(serializedLambda.getImplClass()), serializedLambda.getImplMethodName());
    }

    protected static List<Object> initLocalVariables(SerializedLambda serializedLambda, Object[] params) {
        List<Object> result = new ArrayList<>();
        final Class<?>[] paramTypesFromDescriptor = findParamTypesFromDescriptorAsArray(serializedLambda.getImplMethodSignature());
        final boolean isIoCJobLambda = IocJobLambda.class.getName().equals(toFQClassName(serializedLambda.getFunctionalInterfaceClass()));
        for (int i = 0; i < serializedLambda.getCapturedArgCount(); i++) {
            final Object capturedArg = serializedLambda.getCapturedArg(i);
            result.add(capturedArg);
            if (isPrimitiveLongOrDouble(isIoCJobLambda, paramTypesFromDescriptor, i, capturedArg)) { //why: If the local variable at index is of type double or long, it occupies both index and index + 1. See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html
                result.add(null);
            }
        }
        result.addAll(asList(params));
        if (isIoCJobLambda) {
            result.add(null); // will be injected by IoC
        }
        return result;
    }

    private static boolean isPrimitiveLongOrDouble(boolean isIoCJobLambda, Class<?>[] paramTypesFromDescriptor, int captureArgCounter, Object capturedArg) {
        if (isIoCJobLambda) {
            return isPrimitiveLongOrDouble(paramTypesFromDescriptor[captureArgCounter], capturedArg);
        } else {
            return captureArgCounter > 0 && isPrimitiveLongOrDouble(paramTypesFromDescriptor[captureArgCounter - 1], capturedArg);
        }
    }

    private static boolean isPrimitiveLongOrDouble(Class<?> paramTypeFromDescriptor, Object capturedArg) {
        return paramTypeFromDescriptor.isPrimitive() && (capturedArg instanceof Long || capturedArg instanceof Double);
    }
}
