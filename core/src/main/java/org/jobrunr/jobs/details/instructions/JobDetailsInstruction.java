package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaMethod;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptor;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptorAsArray;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromField;
import static org.jobrunr.utils.reflection.ReflectionUtils.isClassAssignableToObject;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobDetailsInstruction extends VisitMethodInstruction {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobDetailsInstruction.class);

    public JobDetailsInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        if (!isLastJobDetailsInstruction() && isVoidInstruction()) {
            throw new JobRunrException("JobRunr only supports enqueueing/scheduling of one method");
        } else if (isLastJobDetailsInstruction()) {
            jobDetailsBuilder.setClassName(getClassName());
            jobDetailsBuilder.setMethodName(getMethodName());
            jobDetailsBuilder.setJobParameters(getJobParameters());
            return null;
        } else if (owner.startsWith("java")) {
            return getObject();
        } else {
            final long before = System.nanoTime();
            final Object result = getObject();
            final long after = System.nanoTime();
            if ((after - before) > 3_000_000) {
                LOGGER.warn("You are using a custom method ({}.{}({})) while enqueueing that takes a lot of time. See https://www.jobrunr.io/en/documentation/background-methods/best-practices/ on how to use JobRunr effectively.", getClassName(), getMethodName(), Stream.of(findParamTypesFromDescriptorAsArray(descriptor)).map(Class::getSimpleName).collect(joining(", ")));
            }
            return result;
        }
    }

    String getClassName() {
        String className = toFQClassName(owner);
        if (jobDetailsBuilder.getStack().isEmpty()) {
            return findInheritedClassName(className).orElse(className);
        }

        Iterator<Object> objectOnStackDescIterator = jobDetailsBuilder.getStack().descendingIterator();
        while (objectOnStackDescIterator.hasNext()) {
            Object jobOnStack = objectOnStackDescIterator.next();
            if (jobOnStack != null && !jobOnStack.getClass().isSynthetic() && !Proxy.isProxyClass(jobOnStack.getClass())) {
                Class<Object> jobClass = toClass(className);
                if (jobClass.isAssignableFrom(jobOnStack.getClass())) {
                    return jobOnStack.getClass().getName();
                }
            }
        }
        return className;
    }


    String getMethodName() {
        return name;
    }

    protected Object getObject() {
        Class<?>[] paramTypes = findParamTypesFromDescriptorAsArray(descriptor);
        final Object ownerObject = jobDetailsBuilder.getStack().remove(jobDetailsBuilder.getStack().size() - 1 - paramTypes.length);
        return createObjectViaMethod(ownerObject, name, paramTypes, getParametersUsingParamTypes(paramTypes).toArray());
    }

    private Optional<String> findInheritedClassName(String className) {
        if (jobDetailsBuilder.getLocalVariable(0) != null && jobDetailsBuilder.getLocalVariable(0).getClass().getDeclaredFields().length > 0) {
            final Field declaredField = jobDetailsBuilder.getLocalVariable(0).getClass().getDeclaredFields()[0];
            final Object valueFromField = getValueFromField(declaredField, jobDetailsBuilder.getLocalVariable(0));
            if (toClass(className).isAssignableFrom(valueFromField.getClass())) {
                return Optional.of(valueFromField.getClass().getName());
            }
        }
        return Optional.empty();
    }

    protected List<JobParameter> getJobParameters() {
        final List<Class<?>> paramTypesFromDescriptor = findParamTypesFromDescriptor(descriptor);
        final ArrayDeque<Class<?>> paramTypes = new ArrayDeque<>(paramTypesFromDescriptor);

        List<JobParameter> result = new ArrayList<>();
        while (!paramTypes.isEmpty()) {
            result.add(0, toJobParameter(paramTypes.pollLast(), jobDetailsBuilder.getStack().pollLast()));
        }
        return result;
    }

    private JobParameter toJobParameter(Class<?> paramType, Object param) {
        if (param == null) {
            return new JobParameter(paramType, null);
        }

        if (isClassAssignableToObject(paramType, param)) {
            if (boolean.class.equals(paramType) && Integer.class.equals(param.getClass()))
                return new JobParameter(paramType, ((Integer) param) > 0);
            return new JobParameter(paramType, param);
        } else {
            throw shouldNotHappenException(new IllegalStateException("The found parameter types do not match the parameters."));
        }
    }
}
