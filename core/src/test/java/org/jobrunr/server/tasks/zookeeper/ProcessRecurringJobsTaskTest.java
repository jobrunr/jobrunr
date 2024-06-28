package org.jobrunr.server.tasks.zookeeper;

import io.github.artsok.RepeatedIfExceptionsTest;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.storage.RecurringJobsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatJobs;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessRecurringJobsTaskTest extends AbstractTaskTest {

    ProcessRecurringJobsTask task;

    @BeforeEach
    void setUpTask() {
        task = new ProcessRecurringJobsTask(backgroundJobServer);
    }

    @Test
    void testTask() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        runTask(task);

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        Job savedJob = jobsToSaveArgumentCaptor.getValue().get(0);
        assertThat(savedJob)
                .hasState(SCHEDULED)
                .hasRecurringJobId(recurringJob.getId());
    }

    @Test
    void recurringJobsAreCached() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true, false, true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        runTask(task); // initial loading
        verify(storageProvider, times(1)).recurringJobsUpdated(anyLong());
        verify(storageProvider, times(1)).getRecurringJobs();

        runTask(task); // no updates to recurring jobs
        verify(storageProvider, times(2)).recurringJobsUpdated(anyLong());
        verify(storageProvider, times(1)).getRecurringJobs();

        runTask(task); // reload as recurring jobs updated
        verify(storageProvider, times(3)).recurringJobsUpdated(anyLong());
        verify(storageProvider, times(2)).getRecurringJobs();
    }

    @Test
    void taskDoesNotScheduleSameJobIfItIsAlreadyAwaitingScheduledEnqueuedOrProcessed() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/15 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));
        when(storageProvider.countRecurringJobInstances(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(1L);

        runTask(task);

        verify(storageProvider, never()).save(anyList());
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    void taskSchedulesJobsThatWereMissedDuringStopTheWorldGC() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        runTask(task);
        sleep(10500);
        runTask(task);

        verify(storageProvider, times(2)).save(jobsToSaveArgumentCaptor.capture());
        assertThat(jobsToSaveArgumentCaptor.getValue())
                .hasSizeBetween(2, 3)
                .extracting(Job::getState)
                .containsOnly(SCHEDULED);
    }

    @Test
    void taskSchedulesOneExtraJobAheadOfTime() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/15 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        runTask(task);

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        assertThatJobs(jobsToSaveArgumentCaptor.getAllValues().get(0))
                .hasSize(2)
                .allMatch(j -> j.getRecurringJobId().orElse("").equals(recurringJob.getId()))
                .allMatch(j -> j.getState() == SCHEDULED);
    }

    @Test
    void taskKeepsTrackOfRecurringJobRuns() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/15 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR)) {
            runTask(task);

            verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
            assertThatJobs(jobsToSaveArgumentCaptor.getAllValues().get(0))
                    .hasSize(2)
                    .allMatch(j -> j.getRecurringJobId().orElse("").equals(recurringJob.getId()))
                    .allMatch(j -> j.getState() == SCHEDULED);

            assertThat(((ScheduledState) jobsToSaveArgumentCaptor.getValue().get(0).getJobState()).getScheduledAt().plusSeconds(15))
                    .isEqualTo(((ScheduledState) jobsToSaveArgumentCaptor.getValue().get(1).getJobState()).getScheduledAt());
        }


        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR.plusSeconds(15))) {
            Job lastSavedJob = jobsToSaveArgumentCaptor.getValue().get(1);
            clearInvocations(storageProvider);

            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
            assertThat(((ScheduledState) lastSavedJob.getJobState()).getScheduledAt().plusSeconds(15))
                    .isEqualTo(((ScheduledState) jobsToSaveArgumentCaptor.getValue().get(0).getJobState()).getScheduledAt());
        }
    }

    @Test
    void taskTakesSkipsAlreadyScheduledRecurringJobsOnStartup() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/15 * * * * *").build();

        Instant lastScheduledAt = Instant.now().plusSeconds(15);
        when(storageProvider.getRecurringJobsLatestScheduledRun()).thenReturn(Map.of(recurringJob.getId(), lastScheduledAt));
        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        ProcessRecurringJobsTask task = new ProcessRecurringJobsTask(backgroundJobServer);

        runTask(task);

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        Job savedJob = jobsToSaveArgumentCaptor.getValue().get(0);
        assertThat(savedJob)
                .hasState(SCHEDULED)
                .hasRecurringJobId(recurringJob.getId());
        assertThat(savedJob.<ScheduledState>getJobState().getScheduledAt()).isAfter(lastScheduledAt);
    }

    @Test
    void taskDoesNotThrowAnExceptionIfCarbonAwareJobManagerIsNotConfigured() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        assertThat(backgroundJobServer.getCarbonAwareJobManager()).isNull();
        assertThatCode(() -> runTask(task)).doesNotThrowAnyException();
    }
}