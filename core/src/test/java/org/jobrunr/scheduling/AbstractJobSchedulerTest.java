package org.jobrunr.scheduling;


import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.NO_VALIDATE;

class AbstractJobSchedulerTest {

    @Test
    void scheduleRecurrentlyValidatesScheduleDoesNotThrowExceptionWhenUsingInMemoryStorageProvider() {
        AbstractJobScheduler jobScheduler = jobScheduler(new InMemoryStorageProvider());
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("* * * * * *").build();
        assertThatCode(() -> jobScheduler.scheduleRecurrently(recurringJob)).doesNotThrowAnyException();
    }

    @Test
    void scheduleRecurrentlyValidatesScheduleDoesThrowExceptionWhenUsingNotAnInMemoryStorageProvider() {
        AbstractJobScheduler jobScheduler = jobScheduler(new H2StorageProvider(null, NO_VALIDATE));
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("* * * * * *").build();
        assertThatCode(() -> jobScheduler.scheduleRecurrently(recurringJob))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The smallest supported duration between recurring job instances is 5 seconds (because of the smallest supported pollInterval).");
    }

    AbstractJobScheduler jobScheduler(StorageProvider storageProvider) {
        return new AbstractJobScheduler(storageProvider, emptyList()) {
            @Override
            JobId create(JobBuilder jobBuilder) {
                return null;
            }

            @Override
            void create(Stream<JobBuilder> jobBuilderStream) {

            }

            @Override
            String createRecurrently(RecurringJobBuilder recurringJobBuilder) {
                return null;
            }
        };
    }
}