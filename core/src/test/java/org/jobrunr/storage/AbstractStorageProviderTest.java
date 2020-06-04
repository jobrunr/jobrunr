package org.jobrunr.storage;

import org.assertj.core.api.Condition;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobId;
import org.jobrunr.storage.listeners.JobChangeListener;
import org.jobrunr.storage.listeners.JobStatsChangeListener;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Timer;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.Mockito.times;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

class AbstractStorageProviderTest {

    Condition<JobChangeListenerForTest> closeCalled = new Condition<>(x -> x.closeIsCalled, "Close is called");
    Condition<JobChangeListenerForTest> jobNotNull = new Condition<>(x -> x.job != null, "Has Job");

    private SimpleStorageProvider storageProvider;

    @BeforeEach
    void setUpStorageProvider() {
        this.storageProvider = Mockito.spy(new SimpleStorageProvider());
        this.storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
    }

    @Test
    void JobStatsChangeListenersAreNotifiedOfJobStats() {
        final JobStatsChangeListenerForTest changeListener = new JobStatsChangeListenerForTest();
        storageProvider.addJobStorageOnChangeListener(changeListener);
        await()
                .untilAsserted(() -> assertThat(changeListener.jobStats).isNotNull());
    }

    @Test
    void JobChangeListenersAreNotifiedOfJobs() {
        final Job job = anEnqueuedJob().build();
        storageProvider.save(job);
        final JobChangeListenerForTest changeListener = new JobChangeListenerForTest(new JobId(job.getId()));
        storageProvider.addJobStorageOnChangeListener(changeListener);
        await()
                .untilAsserted(() -> assertThat(changeListener).has(jobNotNull));
    }

    @Test
    void ifMultipleJobChangeListenersForSameJobStillOneDatabaseCall() {
        final Job job = anEnqueuedJob().build();
        storageProvider.save(job);

        final JobChangeListenerForTest changeListener1 = new JobChangeListenerForTest(new JobId(job.getId()));
        final JobChangeListenerForTest changeListener2 = new JobChangeListenerForTest(new JobId(job.getId()));
        storageProvider.addJobStorageOnChangeListener(changeListener1);
        storageProvider.addJobStorageOnChangeListener(changeListener2);

        await().untilAsserted(() -> assertThat(changeListener1.job).isNotNull());
        await().untilAsserted(() -> assertThat(changeListener2.job).isNotNull());

        Mockito.verify(storageProvider, times(1)).getJobById(job.getId());
    }

    @Test
    void JobChangeListenersAreClosedIfJobDoesNotExist() {
        final JobChangeListenerForTest changeListener = new JobChangeListenerForTest(new JobId(UUID.randomUUID()));
        storageProvider.addJobStorageOnChangeListener(changeListener);
        await()
                .untilAsserted(() -> assertThat(changeListener).has(closeCalled));
    }

    @Test
    void updateTimerIsStoppedIfNoChangeListeners() {
        final JobStatsChangeListenerForTest changeListener = new JobStatsChangeListenerForTest();

        storageProvider.addJobStorageOnChangeListener(changeListener);
        final Timer timerAfterAddingChangeListener = getInternalState(storageProvider, "timer");
        assertThat(timerAfterAddingChangeListener).isNotNull();

        storageProvider.removeJobStorageOnChangeListener(changeListener);
        final Timer timerAfterRemovigChangeListener = getInternalState(storageProvider, "timer");
        assertThat(timerAfterRemovigChangeListener).isNull();
    }

    private static class JobStatsChangeListenerForTest implements JobStatsChangeListener {

        private JobStats jobStats;

        @Override
        public void onChange(JobStats jobStats) {
            this.jobStats = jobStats;
        }
    }

    private static class JobChangeListenerForTest implements JobChangeListener {

        private final JobId jobId;
        private boolean closeIsCalled;
        private Job job;

        public JobChangeListenerForTest(JobId jobId) {
            this.jobId = jobId;
            this.closeIsCalled = false;
        }

        @Override
        public JobId getJobId() {
            return jobId;
        }

        @Override
        public void onChange(Job job) {
            this.job = job;
        }

        @Override
        public void close() throws Exception {
            this.closeIsCalled = true;
        }
    }

}