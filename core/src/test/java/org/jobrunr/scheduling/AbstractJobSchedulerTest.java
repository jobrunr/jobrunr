package org.jobrunr.scheduling;


import org.jobrunr.jobs.JobDetailsTestBuilder;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.jobrunr.utils.InstantUtils;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.chrono.HijrahDate;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.NO_VALIDATE;

class AbstractJobSchedulerTest {

    @Test
    void scheduleValidatesTemporalType() {
        var storageProvider = new InMemoryStorageProvider();
        try (var mockedInstantUtils = Mockito.mockStatic(InstantUtils.class, Mockito.CALLS_REAL_METHODS)) {
            storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
            AbstractJobScheduler jobScheduler = jobScheduler(storageProvider);

            assertThatCode(() -> jobScheduler.schedule(null, Instant.now(), JobDetailsTestBuilder.defaultJobDetails().build())).doesNotThrowAnyException();

            assertThatCode(() -> jobScheduler.schedule(null, HijrahDate.now(), JobDetailsTestBuilder.defaultJobDetails().build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("JobRunr does not support Temporal type: java.time.chrono.HijrahDate. Supported types are Instant, LocalDateTime, OffsetDateTime and ZonedDateTime.");

            mockedInstantUtils.verify(() -> InstantUtils.toInstant(ArgumentMatchers.any()), Mockito.times(2));
        }
    }

    @Test
    void scheduleRecurrentlyValidatesScheduleDoesNotThrowExceptionWhenUsingInMemoryStorageProvider() {
        AbstractJobScheduler jobScheduler = jobScheduler(new InMemoryStorageProvider());
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("* * * * * *").build();
        assertThatCode(() -> jobScheduler.scheduleRecurrently(recurringJob)).doesNotThrowAnyException();
    }

    @Test
    void scheduleRecurrentlyValidatesScheduleDoesThrowExceptionWhenUsingNotAnH2StorageProvider() {
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