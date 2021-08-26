package org.jobrunr.micronaut.autoconfigure;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import jakarta.inject.Singleton;
import org.jobrunr.micronaut.annotations.Recurring;
import org.jobrunr.scheduling.JobRunrRecurringJobScheduler;
import org.jobrunr.scheduling.JobScheduler;


@Singleton
@Requires(beans = {JobScheduler.class})
public class RecurringMethodProcessor implements ExecutableMethodProcessor<Recurring> {

    private final JobRunrRecurringJobScheduler jobScheduler;

    public RecurringMethodProcessor(JobScheduler jobScheduler) {
        this.jobScheduler = new JobRunrRecurringJobScheduler(jobScheduler);
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        jobScheduler.schedule(method);
    }
}
