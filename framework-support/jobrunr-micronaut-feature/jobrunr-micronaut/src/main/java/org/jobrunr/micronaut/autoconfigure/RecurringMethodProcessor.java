package org.jobrunr.micronaut.autoconfigure;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.scheduling.JobRunrRecurringJobScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Requires(beans = {JobScheduler.class})
public class RecurringMethodProcessor implements ExecutableMethodProcessor<Recurring> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecurringMethodProcessor.class);

    private final JobRunrRecurringJobScheduler jobScheduler;

    public RecurringMethodProcessor(JobScheduler jobScheduler) {
        this.jobScheduler = new JobRunrRecurringJobScheduler(jobScheduler);
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        LOGGER.debug("Registering Recurring Job {}.{}", method.getTargetMethod().getDeclaringClass().getName(), method.getTargetMethod().getName());
        jobScheduler.schedule(method);
    }
}
