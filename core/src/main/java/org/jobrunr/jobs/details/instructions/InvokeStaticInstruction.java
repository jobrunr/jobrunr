package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.*;
import static org.jobrunr.utils.reflection.ReflectionUtils.isClassAssignableToObject;

public class InvokeStaticInstruction extends VisitMethodInstruction {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvokeStaticInstruction.class);

    public InvokeStaticInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        if (isLastInstruction()) {
            jobDetailsBuilder.setClassName(getClassName());
            jobDetailsBuilder.setMethodName(getMethodName());
            jobDetailsBuilder.setJobParameters(getJobParameters());
            return null;
        } else {
            final long before = System.nanoTime();
            final Object result = getObject();
            final long after = System.nanoTime();
            if ((after - before) > 3_000_000) {
                LOGGER.warn("You are using a custom method ({}.{}({})) while enqueueing that takes a lot of time. See https://www.jobrunr.io/en/documentation/background-methods/best-practices/ on how to use JobRunr effectively.", getClassName(), getMethodName(), Stream.of(findParamTypesFromDescriptorAsArray(descriptor)).map(Class::getSimpleName).collect(joining(", ")));
            }
            return isVoidInstruction() ? AbstractJVMInstruction.DO_NOT_PUT_ON_STACK : result;
        }
    }

    private Object getObject() {
        Class<?>[] paramTypes = findParamTypesFromDescriptorAsArray(descriptor);
        List<Object> parameters = getParametersUsingParamTypes(paramTypes);
        if(isKotlinNullCheck()) return null;
        Object result = createObjectViaStaticMethod(getClassName(), getMethodName(), paramTypes, parameters.toArray());
        return result;
    }

    private boolean isKotlinNullCheck() {
        return getClassName().startsWith("kotlin.") && getMethodName().equals("checkNotNullParameter");
    }

    private String getMethodName() {
        return name;
    }

    private String getClassName() {
        return toFQClassName(owner);
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
            if (boolean.class.equals(paramType) && Integer.class.equals(param.getClass()))
                return new JobParameter(paramType, ((Integer) param) > 0);
            return new JobParameter(paramType, param);
        } else {
            throw shouldNotHappenException(new IllegalStateException("The found parameter types do not match the parameters."));
        }
    }
}
