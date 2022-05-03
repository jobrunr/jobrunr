package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static java.util.Arrays.stream;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.*;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromFieldOrProperty;
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

            Object objectViaConstructor = createObjectViaConstructor(className, paramTypes, parameters.toArray());
            if(isKotlinMethodReference(objectViaConstructor)) {
                jobDetailsBuilder.setClassName(((Class)getValueFromFieldOrProperty(objectViaConstructor, "owner")).getName());
                jobDetailsBuilder.setMethodName((String) getValueFromFieldOrProperty(objectViaConstructor, "name"));
            }
            return objectViaConstructor;
        }

        String className = toFQClassName(owner);
        Class<?> objectClass = toClass(className);
        Method method = ReflectionUtils.getMethod(objectClass, name, findParamTypesFromDescriptorAsArray(descriptor));
        if (Modifier.isPrivate(method.getModifiers())) {
            throw JobRunrException.invalidLambdaException(new IllegalAccessException(String.format("JobRunr cannot access member \"%s\" of class %s with modifiers \"private\". Please make the method \"public\".", name, className)));
        }

        throw JobRunrException.shouldNotHappenException("Unknown INVOKESPECIAL instruction: " + className + "." + name);
    }

    private boolean isKotlinMethodReference(Object objectViaConstructor) {
        return stream(objectViaConstructor.getClass().getInterfaces())
                .map(Class::getName)
                .anyMatch(name -> name.startsWith("kotlin.jvm.functions.Function"));
    }

}
