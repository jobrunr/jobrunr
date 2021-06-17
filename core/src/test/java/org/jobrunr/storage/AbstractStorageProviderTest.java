package org.jobrunr.storage;

import org.assertj.core.api.Condition;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.listeners.BackgroundJobServerStatusChangeListener;
import org.jobrunr.storage.listeners.JobChangeListener;
import org.jobrunr.storage.listeners.JobStatsChangeListener;
import org.jobrunr.storage.listeners.MetadataChangeListener;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.Mockito.times;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

class AbstractStorageProviderTest {

    public static final String SOME_METADATA_NAME = "some-metadata-name";
    Condition<JobChangeListenerForTest> closeCalled = new Condition<>(x -> x.closeIsCalled, "Close is called");
    Condition<JobChangeListenerForTest> jobNotNull = new Condition<>(x -> x.job != null, "Has Job");

    private StorageProvider storageProvider;

    @BeforeEach
    void setUpStorageProvider() {
        this.storageProvider = Mockito.spy(new InMemoryStorageProvider());
        this.storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
    }

    @Test
    void jobStatsChangeListenersAreNotifiedOfJobStats() {
        final JobStatsChangeListenerForTest changeListener = new JobStatsChangeListenerForTest();
        storageProvider.addJobStorageOnChangeListener(changeListener);
        await()
                .untilAsserted(() -> assertThat(changeListener.jobStats).isNotNull());
    }

    @Test
    void backgroundJobServerStatusChangeListenersAreNotifiedOfBackgroundJobServers() {
        final BackgroundJobServerStatusChangeListenerForTest changeListener = new BackgroundJobServerStatusChangeListenerForTest();
        storageProvider.addJobStorageOnChangeListener(changeListener);
        await()
                .untilAsserted(() -> assertThat(changeListener.changedServerStates).isNotNull());
    }

    @Test
    void metadataChangeListenersAreNotifiedOfMetadataChanges() {
        final JobRunrMetadata jobRunrMetadata = new JobRunrMetadata(SOME_METADATA_NAME, "some owner", "some value");
        storageProvider.saveMetadata(jobRunrMetadata);
        final MetadataChangeListenerForTest changeListener = new MetadataChangeListenerForTest();
        storageProvider.addJobStorageOnChangeListener(changeListener);
        await()
                .untilAsserted(() -> assertThat(changeListener.metadataList).isNotNull());
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
    void updatingOnChangeListenersIsThreadSafe() {
        List<JobStatsChangeListenerForTest> changeListeners = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final JobStatsChangeListenerForTest changeListener = new SlowJobChangeListenerForTest();
            storageProvider.addJobStorageOnChangeListener(changeListener);
            changeListeners.add(changeListener);
        }

        await().untilAsserted(() -> assertThat(changeListeners).anyMatch(jobStatsChangeListenerForTest -> jobStatsChangeListenerForTest.jobStats != null));
        storageProvider.removeJobStorageOnChangeListener(changeListeners.get(9));
        await().untilAsserted(() -> assertThat(changeListeners).allMatch(jobStatsChangeListenerForTest -> jobStatsChangeListenerForTest.jobStats != null));
    }

    @Test
    void updateTimerIsStoppedIfNoChangeListeners() {
        final JobStatsChangeListenerForTest changeListener = new JobStatsChangeListenerForTest();

        storageProvider.addJobStorageOnChangeListener(changeListener);
        final Timer timerAfterAddingChangeListener = getInternalState(storageProvider, "timer");
        assertThat(timerAfterAddingChangeListener).isNotNull();

        storageProvider.removeJobStorageOnChangeListener(changeListener);
        final Timer timerAfterRemovingChangeListener = getInternalState(storageProvider, "timer");
        assertThat(timerAfterRemovingChangeListener).isNull();
    }

    @Test
    void updateTimerIsStoppedWhenStorageProviderIsStopped() {
        final JobStatsChangeListenerForTest changeListener = new JobStatsChangeListenerForTest();

        storageProvider.addJobStorageOnChangeListener(changeListener);
        final Timer timerAfterAddingChangeListener = getInternalState(storageProvider, "timer");
        assertThat(timerAfterAddingChangeListener).isNotNull();

        storageProvider.close();
        final Timer timerAfterClosingStorageProvider = getInternalState(storageProvider, "timer");
        assertThat(timerAfterClosingStorageProvider).isNull();
    }

    private static class BackgroundJobServerStatusChangeListenerForTest implements BackgroundJobServerStatusChangeListener {

        private List<BackgroundJobServerStatus> changedServerStates;

        @Override
        public void onChange(List<BackgroundJobServerStatus> changedServerStates) {
            this.changedServerStates = changedServerStates;
        }
    }

    private static class MetadataChangeListenerForTest implements MetadataChangeListener {

        private List<JobRunrMetadata> metadataList;

        @Override
        public String listenForChangesOfMetadataName() {
            return SOME_METADATA_NAME;
        }

        @Override
        public void onChange(List<JobRunrMetadata> metadataList) {
            this.metadataList = metadataList;
        }
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
        public void close() {
            this.closeIsCalled = true;
        }
    }

    private static class SlowJobChangeListenerForTest extends JobStatsChangeListenerForTest {

        @Override
        public void onChange(JobStats jobStats) {
            try {
                Thread.sleep(100);
                super.onChange(jobStats);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}