package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

import java.util.List;

import static java.util.Arrays.stream;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaStaticMethod;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptorAsArray;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class InvokeStaticInstruction extends JobDetailsInstruction {

    private static final String[] kotlinCheckMethodNames = {
            "checkNotNull", // not null check
            "throwUninitializedPropertyAccessException" // late init check
    };

    public InvokeStaticInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        if (isKotlinCheck()) {
            getObject();
            return DO_NOT_PUT_ON_STACK;
        }
        return super.invokeInstruction();
    }

    protected Object getObject() {
        Class<?>[] paramTypes = findParamTypesFromDescriptorAsArray(descriptor);
        List<Object> parameters = getParametersUsingParamTypes(paramTypes);
        if (isKotlinCheck()) return null;
        Object result = createObjectViaStaticMethod(getClassName(), getMethodName(), paramTypes, parameters.toArray());
        return result;
    }

    private boolean isKotlinCheck() {
        return getClassName().startsWith("kotlin.")
                && stream(kotlinCheckMethodNames).anyMatch(methodName -> getMethodName().startsWith(methodName));
    }

    String getClassName() {
        return toFQClassName(owner);
    }
}
