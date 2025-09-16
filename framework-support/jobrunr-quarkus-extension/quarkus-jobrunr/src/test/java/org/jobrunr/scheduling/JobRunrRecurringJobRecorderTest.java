package org.jobrunr.scheduling;

import io.quarkus.arc.runtime.BeanContainer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.RecurringJob.CreatedBy.ANNOTATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRunrRecurringJobRecorderTest {

    @Mock
    BeanContainer beanContainer;

    @Mock
    JobScheduler jobScheduler;

    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.JobSchedulerConfiguration jobSchedulerConfiguration;

    @Captor
    ArgumentCaptor<RecurringJob> recurringJobArgumentCaptor;

    @Mock
    ConfigProviderResolver configProviderResolver;
    private ConfigProviderResolver originalResolver;

    @Mock
    Config config;

    JobRunrRecurringJobRecorder jobRunrRecurringJobRecorder;


    @BeforeEach
    void setUpJobRunrRecorder() {
        jobRunrRecurringJobRecorder = new JobRunrRecurringJobRecorder(jobRunrRuntimeConfiguration);
        lenient().when(beanContainer.beanInstance(JobScheduler.class)).thenReturn(jobScheduler);

        when(jobRunrRuntimeConfiguration.jobScheduler()).thenReturn(jobSchedulerConfiguration);
        when(jobSchedulerConfiguration.enabled()).thenReturn(true);

        originalResolver = ConfigProviderResolver.instance();
        ConfigProviderResolver.setInstance(configProviderResolver);
        lenient().when(configProviderResolver.getConfig()).thenReturn(config);
    }

    @AfterEach
    void restoreOriginalResolver() {
        ConfigProviderResolver.setInstance(originalResolver);
    }

    @Test
    void scheduleSchedulesCronJobWithJobRunr() {
        final String id = "my-job-id";
        final JobDetails jobDetails = defaultJobDetails().build();
        final String cron = "*/15 * * * *";
        final String interval = null;
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, cron, interval, zoneId, jobDetails.getClassName(), jobDetails.getMethodName(), jobDetails.getJobParameters());

        // THEN
        verify(jobScheduler).scheduleRecurrently(recurringJobArgumentCaptor.capture());
        final RecurringJob actualRecurringJob = recurringJobArgumentCaptor.getValue();
        assertThat(actualRecurringJob)
                .hasId(id)
                .hasScheduleExpression(cron)
                .hasCreatedBy(ANNOTATION);

        assertThat(actualRecurringJob.getJobDetails())
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(5);
    }

    @Test
    void scheduleSchedulesIntervalJobWithJobRunr() {
        final String id = "my-job-id";
        final JobDetails jobDetails = defaultJobDetails().build();
        final String cron = null;
        final String interval = "PT10M";
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, cron, interval, zoneId, jobDetails.getClassName(), jobDetails.getMethodName(), jobDetails.getJobParameters());

        // THEN
        verify(jobScheduler).scheduleRecurrently(recurringJobArgumentCaptor.capture());
        final RecurringJob actualRecurringJob = recurringJobArgumentCaptor.getValue();
        assertThat(actualRecurringJob)
                .hasId(id)
                .hasScheduleExpression(interval);

        assertThat(actualRecurringJob.getJobDetails())
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(5);
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationCronAndIntervalWillThrowException() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = "*/15 * * * *";
        final String interval = "PT10M";
        final String zoneId = null;

        assertThatThrownBy(() -> jobRunrRecurringJobRecorder.schedule(beanContainer, id, cron, interval, zoneId, jobDetails.getClassName(), jobDetails.getMethodName(), jobDetails.getJobParameters())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationNoCronOrIntervalWillThrowException() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = null;
        final String interval = null;
        final String zoneId = null;

        assertThatThrownBy(() -> jobRunrRecurringJobRecorder.schedule(beanContainer, id, cron, interval, zoneId, jobDetails.getClassName(), jobDetails.getMethodName(), jobDetails.getJobParameters())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scheduleDeletesJobFromJobRunrIfCronExpressionIsCronDisabled() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = "-";
        final String interval = null;
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, cron, interval, zoneId, jobDetails.getClassName(), jobDetails.getMethodName(), jobDetails.getJobParameters());

        verify(jobScheduler).deleteRecurringJob(id);
    }

    @Test
    void scheduleDeletesJobFromJobRunrIfIntervalExpressionIsIntervalDisabled() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = null;
        final String interval = "-";
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, cron, interval, zoneId, jobDetails.getClassName(), jobDetails.getMethodName(), jobDetails.getJobParameters());

        verify(jobScheduler).deleteRecurringJob(id);
    }

    @Test
    void scheduleSchedulesRecurringCronJobWithJobRunrAndResolvesPlaceholders() {
        when(config.getOptionalValue("my-cron-job.cron-expression", String.class)).thenReturn(Optional.of("*/15 * * * *"));

        final String id = "my-job-id";
        final JobDetails jobDetails = defaultJobDetails().build();
        final String cron = "${my-cron-job.cron-expression}";
        final String interval = null;
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, cron, interval, zoneId, jobDetails.getClassName(), jobDetails.getMethodName(), jobDetails.getJobParameters());

        // THEN
        verify(jobScheduler).scheduleRecurrently(recurringJobArgumentCaptor.capture());
        final RecurringJob actualRecurringJob = recurringJobArgumentCaptor.getValue();
        assertThat(actualRecurringJob)
                .hasId(id)
                .hasScheduleExpression("*/15 * * * *");

        assertThat(actualRecurringJob.getJobDetails())
                .hasClass(TestService.class)
                .hasMethodName("doWork");
    }

    @Test
    void scheduleSchedulesRecurringIntervalJobWithJobRunrAndResolvesPlaceholders() {
        when(config.getOptionalValue("my-recurring-job.interval-expression", String.class)).thenReturn(Optional.of("PT10M"));

        final String id = "my-job-id";
        final JobDetails jobDetails = defaultJobDetails().build();
        final String cron = null;
        final String interval = "${my-recurring-job.interval-expression}";
        final String zoneId = null;

        jobRunrRecurringJobRecorder.schedule(beanContainer, id, cron, interval, zoneId, jobDetails.getClassName(), jobDetails.getMethodName(), jobDetails.getJobParameters());

        // THEN
        verify(jobScheduler).scheduleRecurrently(recurringJobArgumentCaptor.capture());
        final RecurringJob actualRecurringJob = recurringJobArgumentCaptor.getValue();
        assertThat(actualRecurringJob)
                .hasId(id)
                .hasScheduleExpression("PT10M");

        assertThat(actualRecurringJob.getJobDetails())
                .hasClass(TestService.class)
                .hasMethodName("doWork");
    }

    @Test
    void scheduleDoesNotSchedulesCronJobWithJobRunrIfJobSchedulerIsNotEnabled() {
        final String id = "my-job-id";
        final JobDetails jobDetails = jobDetails().build();
        final String cron = "*/15 * * * *";
        final String interval = null;
        final String zoneId = null;

        when(jobSchedulerConfiguration.enabled()).thenReturn(false);
        jobRunrRecurringJobRecorder.schedule(beanContainer, id, cron, interval, zoneId, jobDetails.getClassName(), jobDetails.getMethodName(), jobDetails.getJobParameters());

        verify(jobScheduler, never()).scheduleRecurrently(any(RecurringJob.class));
    }
}