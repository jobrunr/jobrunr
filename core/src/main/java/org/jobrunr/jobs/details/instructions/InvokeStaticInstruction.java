package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

import java.util.List;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaStaticMethod;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptorAsArray;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class InvokeStaticInstruction extends VisitMethodInstruction {

    public InvokeStaticInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        String className = toFQClassName(owner);
        String methodName = name;
        Class<?>[] paramTypes = findParamTypesFromDescriptorAsArray(descriptor);
        List<Object> parameters = getParametersUsingParamTypes(paramTypes);

        return createObjectViaStaticMethod(className, methodName, paramTypes, parameters.toArray());
    }

}
