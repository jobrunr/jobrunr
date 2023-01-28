package org.jobrunr.storage;

import org.assertj.core.api.Condition;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.listeners.BackgroundJobServerStatusChangeListener;
import org.jobrunr.storage.listeners.JobChangeListener;
import org.jobrunr.storage.listeners.JobStatsChangeListener;
import org.jobrunr.storage.listeners.MetadataChangeListener;
import org.jobrunr.utils.SleepUtils;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

class AbstractStorageProviderTest {

    public static final String SOME_METADATA_NAME = "some-metadata-name";
    Condition<JobChangeListenerForTest> closeCalled = new Condition<>(x -> x.closeIsCalled, "Close is called");
    Condition<JobChangeListenerForTest> jobNotNull = new Condition<>(x -> x.job != null, "Has Job");

    private AbstractStorageProvider storageProvider;

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
    void jobStatsChangeListenersIsSkippedIfDBIsSlowAndStillFetchingResults() throws InterruptedException {
        // GIVEN
        when(storageProvider.getJobStats()).thenAnswer(i -> {
            sleep(5500);
            return new JobStats(now(), 5L, 0L, 1L, 1L, 0L, 3L, 0L, 0L, 1, 1);
        });

        final JobStatsChangeListenerForTest changeListener = new JobStatsChangeListenerForTest();
        storageProvider.addJobStorageOnChangeListener(changeListener);

        CountDownLatch countDownLatch = new CountDownLatch(5);
        for(int i = 0; i < 5; i++) {
            sleep(1050);
            new Thread(() -> {
                storageProvider.notifyJobStatsOnChangeListeners();
                countDownLatch.countDown();
            }).start();
        }
        countDownLatch.await(5, TimeUnit.SECONDS);

        verify(storageProvider, times(2)).getJobStats();
    }

    @Test
    void jobStatsChangeListenersAreNotifiedInBackgroundThread() {
        final JobStatsChangeListenerForTest changeListener = new JobStatsChangeListenerForTest();
        storageProvider.addJobStorageOnChangeListener(changeListener);

        when(storageProvider.getJobStats()).thenAnswer(i -> {
            SleepUtils.sleep(3000);
            return new JobStats(now(), 5L, 0L, 1L, 1L, 0L, 3L, 0L, 0L, 1, 1);
        });

        long startTimeMillis = System.currentTimeMillis();
        storageProvider.notifyJobStatsOnChangeListeners();
        long endTimeMillis = System.currentTimeMillis();

        assertThat(endTimeMillis - startTimeMillis).isLessThan(1000);
        await().untilAsserted(() -> verify(storageProvider, atLeast(1)).getJobStats());
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