package org.jobrunr.scheduling;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;

import java.util.Collection;

@Singleton
public class AsyncJobValidator {

    private final ApplicationContext applicationContext;

    public AsyncJobValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener
    void startup(StartupEvent event) {
        Collection<BeanDefinition<?>> definitions = applicationContext.getBeanDefinitions(Qualifiers.byStereotype(AsyncJob.class));

        for (BeanDefinition<?> beanDefinition : definitions) {
            var executableMethods = beanDefinition.getExecutableMethods();
            for (ExecutableMethod<?, ?> executableMethod : executableMethods) {
                if (isJobRunrJobWithAReturnValue(executableMethod)) {
                    throw new IllegalArgumentException("An @AsyncJob cannot have a return value. " + beanDefinition.getClass().getName() + "@" + executableMethod.getName() + " is defined as an @AsyncJob but has a return value.");
                }
            }
        }
    }

    private static boolean isJobRunrJobWithAReturnValue(ExecutableMethod<?, ?> method) {
        return method.isAnnotationPresent(Job.class) && !method.getReturnType().getType().equals(Void.TYPE);
    }
}
