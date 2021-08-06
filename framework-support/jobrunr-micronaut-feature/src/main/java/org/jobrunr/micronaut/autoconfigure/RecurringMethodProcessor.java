package org.jobrunr.micronaut.autoconfigure;

import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import org.jobrunr.micronaut.annotations.Recurring;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.MicronautScheduler;

import javax.inject.Singleton;


@Singleton
public class RecurringMethodProcessor implements ExecutableMethodProcessor<Recurring> {

    private final MicronautScheduler jobScheduler;

    public RecurringMethodProcessor(JobScheduler jobScheduler) {
        this.jobScheduler = new MicronautScheduler(jobScheduler);
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        jobScheduler.schedule(method);
    }
}
