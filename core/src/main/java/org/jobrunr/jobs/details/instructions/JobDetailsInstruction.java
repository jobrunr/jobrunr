package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.details.JobDetailsFinderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaMethod;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptor;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptorAsArray;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.isClassAssignableToObject;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class JobDetailsInstruction extends VisitMethodInstruction {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobDetailsAsmGenerator.class);

    public JobDetailsInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        if (isLastInstruction()) {
            jobDetailsBuilder.setClassName(toFQClassName(owner));
            jobDetailsBuilder.setMethodName(name);
            jobDetailsBuilder.setJobParameters(getJobParameters());
            return null;
        } else if (owner.startsWith("java")) {
            return getObject();
        } else {
            final long before = System.nanoTime();
            final Object result = getObject();
            final long after = System.nanoTime();
            if ((after - before) > 1_000_000) {
                LOGGER.warn(String.format("You are using a custom method (%s.%s(%s)) while enqueueing that takes a lot of time. See https://www.jobrunr.io/documentation/best-practices/ on how to use JobRunr effectively.", toFQClassName(owner), name, Stream.of(findParamTypesFromDescriptorAsArray(descriptor)).map(Class::getSimpleName).collect(joining(", "))));
            }
            return result;
        }
    }

    private Object getObject() {
        Class<?>[] paramTypes = findParamTypesFromDescriptorAsArray(descriptor);
        final Object ownerObject = jobDetailsBuilder.getStack().remove(jobDetailsBuilder.getStack().size() - 1 - paramTypes.length);
        return createObjectViaMethod(ownerObject, name, paramTypes, getParametersUsingParamTypes(paramTypes).toArray());
    }

    private boolean isLastInstruction() {
        return jobDetailsBuilder.getInstructions().isEmpty();
    }

    private List<JobParameter> getJobParameters() {
        final List<Class<?>> paramTypesFromDescriptor = findParamTypesFromDescriptor(descriptor);
        final LinkedList<Class<?>> paramTypes = new LinkedList<>(paramTypesFromDescriptor);

        List<JobParameter> result = new ArrayList<>();
        while (!paramTypes.isEmpty()) {
            result.add(0, toJobParameter(paramTypes.pollLast(), jobDetailsBuilder.getStack().pollLast()));
        }
        return result;
    }

    private JobParameter toJobParameter(Class<?> paramType, Object param) {
        if (isClassAssignableToObject(paramType, param)) {
            if (boolean.class.equals(paramType) && Integer.class.equals(param.getClass())) return new JobParameter(paramType, ((Integer) param) > 0);
            return new JobParameter(paramType, param);
        } else {
            throw shouldNotHappenException(new IllegalStateException("The found parameter types do not match the parameters."));
        }
    }
}
