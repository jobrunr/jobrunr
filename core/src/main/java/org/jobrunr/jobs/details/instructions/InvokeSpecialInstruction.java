package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaConstructor;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptorAsArray;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class InvokeSpecialInstruction extends VisitMethodInstruction {

    public InvokeSpecialInstruction(JobDetailsBuilder jobDetailsBuilder) {
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

        String className = toFQClassName(owner);
        Class<?> objectClass = toClass(className);
        Method method = ReflectionUtils.getMethod(objectClass, name, findParamTypesFromDescriptorAsArray(descriptor));
        if (Modifier.isPrivate(method.getModifiers())) {
            throw JobRunrException.invalidLambdaException(new IllegalAccessException(String.format("JobRunr cannot access member \"%s\" of class %s with modifiers \"private\". Please make the method \"public\".", name, className)));
        }

        throw JobRunrException.shouldNotHappenException("Unknown INVOKESPECIAL instruction: " + className + "." + name);
    }

}
