package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

import java.util.List;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaStaticMethod;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptorAsArray;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class InvokeStaticInstruction extends JobDetailsInstruction {

    public InvokeStaticInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        if (isKotlinNullCheck()) {
            getObject();
            return DO_NOT_PUT_ON_STACK;
        }
        return super.invokeInstruction();
    }

    protected Object getObject() {
        Class<?>[] paramTypes = findParamTypesFromDescriptorAsArray(descriptor);
        List<Object> parameters = getParametersUsingParamTypes(paramTypes);
        if (isKotlinNullCheck()) return null;
        Object result = createObjectViaStaticMethod(getClassName(), getMethodName(), paramTypes, parameters.toArray());
        return result;
    }

    private boolean isKotlinNullCheck() {
        return getClassName().startsWith("kotlin.") && (getMethodName().startsWith("checkNotNull"));
    }

    String getClassName() {
        return toFQClassName(owner);
    }
}
