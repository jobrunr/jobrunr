package org.jobrunr.scheduling;

import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ExecutableMethod;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;

@Singleton
public class AsyncJobPostProcessor implements BeanCreatedEventListener<Object> {

    @Override
    public Object onCreated(@NonNull BeanCreatedEvent<Object> event) {
        Object bean = event.getBean();
        Class<?> beanClass = bean.getClass();
        var definition = event.getBeanDefinition();

        if (!beanClass.isAnnotationPresent(AsyncJob.class)) {
            return bean;
        }

        for (var method : definition.getExecutableMethods()) {
            validateMethodAnnotatedWithJobShouldNotHaveAReturnType(method);
        }

        return bean;
    }

    private void validateMethodAnnotatedWithJobShouldNotHaveAReturnType(ExecutableMethod<?, ?> method) {
        if (isJobRunrJobWithAReturnValue(method)) {
            throw new IllegalArgumentException("An @AsyncJob cannot have a return value. " + method + " is defined as an @AsyncJob but has a return value.");
        }
    }

    private static boolean isJobRunrJobWithAReturnValue(ExecutableMethod<?, ?> method) {
        return method.isAnnotationPresent(Job.class) && !method.getReturnType().getType().equals(Void.TYPE);
    }
}
