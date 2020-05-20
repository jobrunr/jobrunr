package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.details.JobDetailsFinderContext;

import java.util.List;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaConstructor;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptorAsArray;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class InvokeSpecialInstruction extends VisitMethodInstruction {

    public InvokeSpecialInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        if ("<init>".equals(name)) {
            String className = toFQClassName(owner);
            Class<?>[] paramTypes = findParamTypesFromDescriptorAsArray(descriptor);
            List<Object> parameters = getParametersUsingParamTypes(paramTypes);

            return createObjectViaConstructor(className, paramTypes, parameters.toArray());
        }
        throw JobRunrException.shouldNotHappenException("Unknown INVOKESPECIAL instruction: " + name);
    }

}
