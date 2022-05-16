package org.jobrunr.scheduling;

import io.quarkus.arc.runtime.BeanContainer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRunrRecurringJobRecorderTest {

    @Mock
    BeanContainer beanContainer;

    @Mock
    JobScheduler jobScheduler;

    @Mock
    ConfigProviderResolver configProviderResolver;

    @Mock
    Config config;

    JobRunrRecurringJobRecorder jobRunrRecurringJobRecorder;

    @BeforeEach
    void setUpJobRunrRecorder() {
        jobRunrRecurringJobRecorder = new JobRunrRecurringJobRecorder();
        when(beanContainer.instance(JobScheduler.class)).thenReturn(jobScheduler);

        ConfigProviderResolver.setInstance(configProviderResolver);
        when(configProviderResolver.getConfig()).thenReturn(config);
    }

    @Test
    void scheduleSchedulesCronJobWithJobRunr() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = "*/15 * * * *";
        final String interval = null;
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, jobDetails, cron, interval, zoneId);

        verify(jobScheduler).scheduleRecurrently(id, jobDetails, CronExpression.create(cron), ZoneId.systemDefault());
    }

    @Test
    void scheduleSchedulesIntervalJobWithJobRunr() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = null;
        final String interval = "PT10M";
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, jobDetails, cron, interval, zoneId);

        verify(jobScheduler).scheduleRecurrently(id, jobDetails, new Interval("PT10M"), ZoneId.systemDefault());
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationCronAndIntervalWillThrowException() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = "*/15 * * * *";
        final String interval = "PT10M";
        final String zoneId = null;

        assertThatThrownBy(() -> jobRunrRecurringJobRecorder.schedule(beanContainer, id, jobDetails, cron, interval, zoneId)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationNoCronOrIntervalWillThrowException() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = null;
        final String interval = null;
        final String zoneId = null;

        assertThatThrownBy(() -> jobRunrRecurringJobRecorder.schedule(beanContainer, id, jobDetails, cron, interval, zoneId)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scheduleDeletesJobFromJobRunrIfCronExpressionIsCronDisabled() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = "-";
        final String interval = null;
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, jobDetails, cron, interval, zoneId);

        verify(jobScheduler).delete(id);
    }

    @Test
    void scheduleDeletesJobFromJobRunrIfIntervalExpressionIsIntervalDisabled() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = null;
        final String interval = "-";
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, jobDetails, cron, interval, zoneId);

        verify(jobScheduler).delete(id);
    }

    @Test
    void scheduleSchedulesRecurringCronJobWithJobRunrAndResolvesPlaceholders() {
        when(config.getOptionalValue("my-cron-job.cron-expression", String.class)).thenReturn(Optional.of("*/15 * * * *"));

        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = "${my-cron-job.cron-expression}";
        final String interval = null;
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, jobDetails, cron, interval, zoneId);

        verify(jobScheduler).scheduleRecurrently(id, jobDetails, CronExpression.create("*/15 * * * *"), ZoneId.systemDefault());
    }

    @Test
    void scheduleSchedulesRecurringIntervalJobWithJobRunrAndResolvesPlaceholders() {
        when(config.getOptionalValue("my-recurring-job.interval-expression", String.class)).thenReturn(Optional.of("PT10M"));

        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = null;
        final String interval = "${my-recurring-job.interval-expression}";
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, jobDetails, cron, interval, zoneId);

        verify(jobScheduler).scheduleRecurrently(id, jobDetails, new Interval("PT10M"), ZoneId.systemDefault());
    }
}