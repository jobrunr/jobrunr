package org.jobrunr.scheduling;


import org.jobrunr.jobs.carbonaware.CarbonAwareJobManager;
import org.jobrunr.jobs.JobDetailsTestBuilder;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.NO_VALIDATE;

class AbstractJobSchedulerTest {

    @Test
    void scheduleCarbonAwareThrowsAnExceptionIfCarbonJobManagerIsNull() {
        AbstractJobScheduler jobScheduler = jobScheduler(new InMemoryStorageProvider(), null);

        assertThatCode(() -> jobScheduler.scheduleCarbonAware(null, CarbonAwarePeriod.after(Instant.now().plus(4, ChronoUnit.HOURS)), JobDetailsTestBuilder.defaultJobDetails().build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CarbonAwareJobManager is not configured. Cannot schedule carbon aware jobs.");
    }

    @Test
    void scheduleCarbonAwareDoesNotThrowIfCarbonJobManagerIsConfigured() {
        JsonMapper jsonMapper = new JacksonJsonMapper();
        StorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(jsonMapper));
        AbstractJobScheduler jobScheduler = jobScheduler(storageProvider, new CarbonAwareJobManager(usingStandardCarbonAwareConfiguration(), jsonMapper));

        assertThatCode(() -> jobScheduler.scheduleCarbonAware(null, CarbonAwarePeriod.after(Instant.now().plus(4, ChronoUnit.HOURS)), JobDetailsTestBuilder.defaultJobDetails().build()))
                .doesNotThrowAnyException();
    }

    @Test
    void scheduleRecurrentlyValidatesScheduleDoesNotThrowExceptionWhenUsingInMemoryStorageProvider() {
        AbstractJobScheduler jobScheduler = jobScheduler(new InMemoryStorageProvider(), null);
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("* * * * * *").build();
        assertThatCode(() -> jobScheduler.scheduleRecurrently(recurringJob)).doesNotThrowAnyException();
    }

    @Test
    void scheduleRecurrentlyValidatesScheduleDoesThrowExceptionWhenUsingNotAnInMemoryStorageProvider() {
        AbstractJobScheduler jobScheduler = jobScheduler(new H2StorageProvider(null, NO_VALIDATE), null);
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("* * * * * *").build();
        assertThatCode(() -> jobScheduler.scheduleRecurrently(recurringJob))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The smallest supported duration between recurring job instances is 5 seconds (because of the smallest supported pollInterval).");
    }

    AbstractJobScheduler jobScheduler(StorageProvider storageProvider, CarbonAwareJobManager carbonAwareJobManager) {
        return new AbstractJobScheduler(storageProvider, carbonAwareJobManager, emptyList()) {
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