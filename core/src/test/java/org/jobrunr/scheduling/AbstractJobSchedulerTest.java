package org.jobrunr.scheduling;


import org.jobrunr.jobs.JobDetailsTestBuilder;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.ThaiBuddhistDate;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.NO_VALIDATE;

class AbstractJobSchedulerTest {

    @Test
    void scheduleValidatesTemporalType() {
        var storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        AbstractJobScheduler jobScheduler = jobScheduler(storageProvider);
        assertThatCode(() -> jobScheduler.schedule(null, Instant.now(), JobDetailsTestBuilder.defaultJobDetails().build())).doesNotThrowAnyException();
        assertThatCode(() -> jobScheduler.schedule(null, LocalDateTime.now(), JobDetailsTestBuilder.defaultJobDetails().build())).doesNotThrowAnyException();
        assertThatCode(() -> jobScheduler.schedule(null, OffsetDateTime.now(), JobDetailsTestBuilder.defaultJobDetails().build())).doesNotThrowAnyException();
        assertThatCode(() -> jobScheduler.schedule(null, ZonedDateTime.now(), JobDetailsTestBuilder.defaultJobDetails().build())).doesNotThrowAnyException();

        assertThatCode(() -> jobScheduler.schedule(null, HijrahDate.now(), JobDetailsTestBuilder.defaultJobDetails().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Job scheduling failed: the Temporal type 'java.time.chrono.HijrahDate' is not supported. Please provide either an Instant, a LocalDateTime, a ZonedDateTime or an OffsetDateTime.");
        assertThatCode(() -> jobScheduler.schedule(null, JapaneseDate.now(), JobDetailsTestBuilder.defaultJobDetails().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Job scheduling failed: the Temporal type 'java.time.chrono.JapaneseDate' is not supported. Please provide either an Instant, a LocalDateTime, a ZonedDateTime or an OffsetDateTime.");
        assertThatCode(() -> jobScheduler.schedule(null, LocalTime.now(), JobDetailsTestBuilder.defaultJobDetails().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Job scheduling failed: the Temporal type 'java.time.LocalTime' is not supported. Please provide either an Instant, a LocalDateTime, a ZonedDateTime or an OffsetDateTime.");
        assertThatCode(() -> jobScheduler.schedule(null, LocalDate.now(), JobDetailsTestBuilder.defaultJobDetails().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Job scheduling failed: the Temporal type 'java.time.LocalDate' is not supported. Please provide either an Instant, a LocalDateTime, a ZonedDateTime or an OffsetDateTime.");
        assertThatCode(() -> jobScheduler.schedule(null, Year.now(), JobDetailsTestBuilder.defaultJobDetails().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Job scheduling failed: the Temporal type 'java.time.Year' is not supported. Please provide either an Instant, a LocalDateTime, a ZonedDateTime or an OffsetDateTime.");
        assertThatCode(() -> jobScheduler.schedule(null, YearMonth.now(), JobDetailsTestBuilder.defaultJobDetails().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Job scheduling failed: the Temporal type 'java.time.YearMonth' is not supported. Please provide either an Instant, a LocalDateTime, a ZonedDateTime or an OffsetDateTime.");
        assertThatCode(() -> jobScheduler.schedule(null, OffsetTime.now(), JobDetailsTestBuilder.defaultJobDetails().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Job scheduling failed: the Temporal type 'java.time.OffsetTime' is not supported. Please provide either an Instant, a LocalDateTime, a ZonedDateTime or an OffsetDateTime.");
        assertThatCode(() -> jobScheduler.schedule(null, ThaiBuddhistDate.now(), JobDetailsTestBuilder.defaultJobDetails().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Job scheduling failed: the Temporal type 'java.time.chrono.ThaiBuddhistDate' is not supported. Please provide either an Instant, a LocalDateTime, a ZonedDateTime or an OffsetDateTime.");
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