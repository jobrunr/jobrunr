package org.jobrunr.scheduling;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.ApplyStateFilter;
import org.jobrunr.jobs.filters.ElectStateFilter;
import org.jobrunr.jobs.filters.JobClientFilter;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.jobs.carbonaware.CarbonAwareJobManager;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobSchedulerTest {

    @Mock
    private StorageProvider storageProvider;
    @Mock
    CarbonAwareJobManager carbonAwareJobManager;

    private TestService testService;
    private JobScheduler jobScheduler;
    private JobClientLogFilter jobClientLogFilter;

    @BeforeEach
    void setupTestService() {
        testService = new TestService();

        jobClientLogFilter = new JobClientLogFilter();
        jobScheduler = new JobScheduler(storageProvider, carbonAwareJobManager, List.of(jobClientLogFilter));
    }

    @Test
    void onSaveJobCreatingAndCreatedAreCalled() {
        when(storageProvider.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jobScheduler.enqueue(() -> testService.doWork());

        assertThat(jobClientLogFilter.onCreating).isTrue();
        assertThat(jobClientLogFilter.onCreated).isTrue();
    }

    @Test
    void onSaveJobMDCDataIsPutIntoJob() {
        ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);

        MDC.put("some-key", "some-value");
        when(storageProvider.save(jobArgumentCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        jobScheduler.enqueue(() -> testService.doWork());

        Job job = jobArgumentCaptor.getValue();
        assertThat(job.getMetadata()).containsKey("mdc-some-key");
    }

    @Test
    void onDeleteJobStateElectionAndStateAppliedAreCalled() {
        final Job enqueuedJob = anEnqueuedJob().build();
        when(storageProvider.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageProvider.getJobById(enqueuedJob.getId())).thenReturn(enqueuedJob);

        jobScheduler.delete(enqueuedJob.getId());

        assertThat(jobClientLogFilter.onStateElection).isTrue();
        assertThat(jobClientLogFilter.onStateApplied).isTrue();
        verify(storageProvider).save(any(Job.class));
    }

    @Test
    void onStreamOfJobsCreatingAndCreatedAreCalled() {
        when(storageProvider.save(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        final Stream<Integer> range = IntStream.range(0, 1).boxed();
        jobScheduler.enqueue(range, (i) -> testService.doWork(i));

        assertThat(jobClientLogFilter.onCreating).isTrue();
        assertThat(jobClientLogFilter.onCreated).isTrue();
    }

    @Test
    void onRecurringJobCreatingAndCreatedAreCalled() {
        when(storageProvider.saveRecurringJob(any(RecurringJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jobScheduler.scheduleRecurrently(Cron.daily(), () -> testService.doWork());

        assertThat(jobClientLogFilter.onCreating).isTrue();
        assertThat(jobClientLogFilter.onCreated).isTrue();
    }

    private static class JobClientLogFilter implements JobClientFilter, ElectStateFilter, ApplyStateFilter {

        private boolean onCreating;
        private boolean onCreated;
        private boolean onStateElection;
        private boolean onStateApplied;

        @Override
        public void onCreating(AbstractJob job) {
            onCreating = true;
        }

        @Override
        public void onCreated(AbstractJob job) {
            onCreated = true;
        }

        @Override
        public void onStateElection(Job job, JobState newState) {
            onStateElection = true;
        }

        @Override
        public void onStateApplied(Job job, JobState oldState, JobState newState) {
            onStateApplied = true;
        }
    }

}