package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.details.JobDetailsFinderContext;
import org.jobrunr.utils.streams.StreamUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaMethod;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptor;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptorAsArray;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.isClassAssignableToObject;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class JobDetailsInstruction extends VisitMethodInstruction {

    public JobDetailsInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        Class[] paramTypes = findParamTypesFromDescriptorAsArray(descriptor);
        Object object = jobDetailsBuilder.pollLastInstruction().invokeInstruction();
        return createObjectViaMethod(object, name, paramTypes, getParametersUsingParamTypes(paramTypes).toArray());
    }

    public JobDetails getJobDetails() {
        String className = toFQClassName(owner);
        String staticFieldName = null;
        String methodName = name;
        final List<JobParameter> jobParameters = getJobParameters();

        Optional<GetStaticInstruction> staticInstructionOptional = getStaticInstruction();
        if (staticInstructionOptional.isPresent()) {
            final GetStaticInstruction staticInstruction = staticInstructionOptional.get();
            className = toFQClassName(staticInstruction.owner);
            staticFieldName = staticInstruction.name;
        }
        return new JobDetails(className, staticFieldName, methodName, jobParameters);
    }

    private Optional<GetStaticInstruction> getStaticInstruction() {
        return StreamUtils.ofType(jobDetailsBuilder.getInstructions(), GetStaticInstruction.class)
                .findFirst();
    }

    private List<JobParameter> getJobParameters() {
        final LinkedList<Class> paramTypes = new LinkedList<>(findParamTypesFromDescriptor(descriptor));

        List<JobParameter> result = new ArrayList<>();
        while (!paramTypes.isEmpty()) {
            result.add(0, toJobParameter(paramTypes.pollLast(), jobDetailsBuilder.pollLastInstruction().invokeInstruction()));
        }
        return result;
    }

    private JobParameter toJobParameter(Class paramType, Object param) {
        if (isClassAssignableToObject(paramType, param)) {
            if (boolean.class.equals(paramType) && Integer.class.equals(param.getClass())) return new JobParameter(paramType, ((Integer) param) > 0);
            return new JobParameter(paramType, param);
        } else {
            throw shouldNotHappenException(new IllegalStateException("The found parameter types do not match the parameters."));
        }
    }
}
