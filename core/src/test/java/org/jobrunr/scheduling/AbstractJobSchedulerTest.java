package org.jobrunr.scheduling;


import org.jobrunr.jobs.JobDetailsTestBuilder;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.InstantUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.chrono.HijrahDate;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThatJobs;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnCreatedAt;
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
                    .hasMessage("JobRunr does not support Temporal type: java.time.chrono.HijrahDate. Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");

            mockedInstantUtils.verify(() -> InstantUtils.toInstant(ArgumentMatchers.any()), Mockito.times(2));
        }
    }

    StorageProvider storageProvider;

    @Test
    void scheduleCarbonAwareSavesJobInAwaitingState() {
        AbstractJobScheduler jobScheduler = jobScheduler();

        jobScheduler.scheduleCarbonAware(null, CarbonAwarePeriod.before(Instant.now().plus(4, ChronoUnit.HOURS)), JobDetailsTestBuilder.defaultJobDetails().build());

        assertThatJobs(storageProvider.getJobList(StateName.AWAITING, ascOnCreatedAt(10))).hasSize(1);
    }

    @Test
    void scheduleRecurrentlyValidatesScheduleDoesNotThrowExceptionWhenUsingInMemoryStorageProvider() {
        AbstractJobScheduler jobScheduler = jobScheduler();
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

    @Test
    void scheduleRecurrentlyValidatesScheduleDoesThrowExceptionWhenScheduleIntervalIsLowerCarbonAwareMargin() {
        AbstractJobScheduler jobScheduler = jobScheduler();
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("* * * * * * [PT0S/PT1M]").build();
        assertThatCode(() -> jobScheduler.scheduleRecurrently(recurringJob))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The total carbon aware margin must be lower than the duration between each schedule.");
    }

    AbstractJobScheduler jobScheduler() {
        JsonMapper jsonMapper = new JacksonJsonMapper();
        StorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(jsonMapper));

        return jobScheduler(storageProvider);
    }

    AbstractJobScheduler jobScheduler(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
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