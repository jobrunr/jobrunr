package org.jobrunr.storage;

import io.github.artsok.RepeatedIfExceptionsTest;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.jobrunr.storage.Paging.AmountBasedList;
import org.jobrunr.storage.Paging.OffsetBasedPage;
import org.jobrunr.storage.listeners.JobStatsChangeListener;
import org.jobrunr.storage.listeners.MetadataChangeListener;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.stubs.BackgroundJobServerStub;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.exceptions.Exceptions;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThatJobs;
import static org.jobrunr.JobRunrAssertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.failedJob;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aCarbonAwaitingJob;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.aDeletedJob;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aBackgroundJobServerStatusBasedOn;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.jobrunr.utils.streams.StreamUtils.batchCollector;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@ExtendWith(MockitoExtension.class)
public abstract class StorageProviderTest {

    protected StorageProvider storageProvider;
    protected BackgroundJobServer backgroundJobServer;
    protected JobMapper jobMapper;

    protected BackgroundJobServerConfigurationReader backgroundJobServerConfiguration;

    @BeforeEach
    public void cleanUpAndSetupBackgroundJobServer() {
        cleanup();
        final JacksonJsonMapper jsonMapper = new JacksonJsonMapper();
        JobRunr.configure()
                .useStorageProvider(getStorageProvider())
                .initialize();
        storageProvider = getStorageProvider();
        assertThat(storageProvider).isNotNull();
        backgroundJobServerConfiguration = spy(new BackgroundJobServerConfigurationReader(usingStandardBackgroundJobServerConfiguration()));

        backgroundJobServer = new BackgroundJobServerStub(storageProvider, jsonMapper, backgroundJobServerConfiguration, null);
        jobMapper = new JobMapper(jsonMapper);
    }

    @AfterEach
    public void cleanupStorageProvider() {
        this.storageProvider.close();
    }

    protected abstract void cleanup();

    protected abstract StorageProvider getStorageProvider();

    protected ThrowingStorageProvider makeThrowingStorageProvider(StorageProvider storageProvider) {
        return new ThrowingStorageProvider(storageProvider, "TODO") {
            @Override
            protected void makeStorageProviderThrowException(StorageProvider storageProvider) {
                throw new UnsupportedOperationException("Implement me!");
            }
        };
    }

    @Test
    void testAnnounceAndListBackgroundJobServers() {
        final BackgroundJobServerStatus serverStatus1 = aDefaultBackgroundJobServerStatus().withName("server-A").withIsStarted().build();
        storageProvider.announceBackgroundJobServer(serverStatus1);
        sleep(100);

        final BackgroundJobServerStatus serverStatus2 = aDefaultBackgroundJobServerStatus().withName("server-B").withIsStarted().build();
        storageProvider.announceBackgroundJobServer(serverStatus2);
        sleep(100);

        storageProvider.signalBackgroundJobServerAlive(aBackgroundJobServerStatusBasedOn(serverStatus2).withLastHeartbeat(now()).build());
        sleep(10);
        storageProvider.signalBackgroundJobServerAlive(aBackgroundJobServerStatusBasedOn(serverStatus1).withLastHeartbeat(now()).build());

        final List<BackgroundJobServerStatus> backgroundJobServers = storageProvider.getBackgroundJobServers();

        assertThat(backgroundJobServers).hasSize(2);
        //why: sqlite has no microseconds precision for timestamps
        assertThat(backgroundJobServers.get(0)).isEqualToComparingOnlyGivenFields(serverStatus1, "id", "workerPoolSize", "pollIntervalInSeconds", "running");
        assertThat(backgroundJobServers.get(1)).isEqualToComparingOnlyGivenFields(serverStatus2, "id", "workerPoolSize", "pollIntervalInSeconds", "running");
        assertThat(backgroundJobServers.get(0).getFirstHeartbeat()).isCloseTo(serverStatus1.getFirstHeartbeat(), within(1000, MICROS));
        assertThat(backgroundJobServers.get(0).getLastHeartbeat()).isAfter(backgroundJobServers.get(0).getFirstHeartbeat());
        assertThat(backgroundJobServers.get(1).getFirstHeartbeat()).isCloseTo(serverStatus2.getFirstHeartbeat(), within(1000, MICROS));
        assertThat(backgroundJobServers.get(1).getLastHeartbeat()).isAfter(backgroundJobServers.get(1).getFirstHeartbeat());
        assertThat(backgroundJobServers).extracting("id").containsExactly(serverStatus1.getId(), serverStatus2.getId());
        assertThat(backgroundJobServers).extracting("name").containsExactly(serverStatus1.getName(), serverStatus2.getName());

        assertThat(storageProvider.getLongestRunningBackgroundJobServerId()).isEqualTo(serverStatus1.getId());

        storageProvider.signalBackgroundJobServerStopped(serverStatus1);
        assertThat(storageProvider.getBackgroundJobServers()).hasSize(1);
        assertThat(storageProvider.getLongestRunningBackgroundJobServerId()).isEqualTo(serverStatus2.getId());
    }

    @Test
    void testRemoveTimedOutBackgroundJobServers() {
        final BackgroundJobServerStatus serverStatus1 = aDefaultBackgroundJobServerStatus().withIsStarted().build();
        storageProvider.announceBackgroundJobServer(serverStatus1);
        sleep(50);
        Instant deleteServersWithHeartbeatOlderThanThis = now();
        sleep(50);

        final BackgroundJobServerStatus serverStatus2 = aDefaultBackgroundJobServerStatus().withIsStarted().build();
        storageProvider.announceBackgroundJobServer(serverStatus2);

        final int deletedServers = storageProvider.removeTimedOutBackgroundJobServers(deleteServersWithHeartbeatOlderThanThis);
        assertThat(deletedServers).isEqualTo(1);
        assertThat(storageProvider.getBackgroundJobServers()).hasSize(1);
        assertThat(storageProvider.getBackgroundJobServers().get(0).getId()).isEqualTo(serverStatus2.getId());
    }

    @Test
    void ifServerHasTimedOutAndSignalsItsAliveAnExceptionIsThrown() {
        final BackgroundJobServerStatus serverStatus = aDefaultBackgroundJobServerStatus().withIsStarted().build();
        storageProvider.announceBackgroundJobServer(serverStatus);
        sleep(100);

        Instant deleteServersWithHeartbeatOlderThanThis = now();
        storageProvider.removeTimedOutBackgroundJobServers(deleteServersWithHeartbeatOlderThanThis);

        assertThatThrownBy(() -> storageProvider.signalBackgroundJobServerAlive(serverStatus)).isInstanceOf(ServerTimedOutException.class);
    }

    @Test
    void testCRUDMetadataLifeCycle() {
        List<JobRunrMetadata> metadataListBeforeCreate = storageProvider.getMetadata("shouldNotHappenException");
        assertThat(metadataListBeforeCreate).isEmpty();

        // CREATE
        JobRunrMetadata metadata1 = new JobRunrMetadata("shouldNotHappenException", UUID.randomUUID().toString(), Exceptions.getStackTraceAsString(shouldNotHappenException("bad!")));
        JobRunrMetadata metadata2 = new JobRunrMetadata("shouldNotHappenException", UUID.randomUUID().toString(), Exceptions.getStackTraceAsString(shouldNotHappenException("Really bad!")));
        storageProvider.saveMetadata(metadata1);
        storageProvider.saveMetadata(metadata2);

        // LIST
        List<JobRunrMetadata> metadataListAfterCreate = storageProvider.getMetadata("shouldNotHappenException");
        assertThat(metadataListAfterCreate).hasSize(2);

        // GET
        assertThat(storageProvider.getMetadata("shouldNotHappenException", metadata1.getOwner())).isEqualTo(metadata1);
        assertThat(storageProvider.getMetadata("shouldNotHappenException", metadata2.getOwner())).isEqualTo(metadata2);
        assertThat(storageProvider.getMetadata("somethingThatDoesNotExist", UUID.randomUUID().toString())).isNull();

        // UPDATE
        JobRunrMetadata metadata1Update = new JobRunrMetadata("shouldNotHappenException", metadata1.getOwner(), "An Update");
        JobRunrMetadata metadata2Update = new JobRunrMetadata("shouldNotHappenException", metadata2.getOwner(), "An Update");
        storageProvider.saveMetadata(metadata1Update);
        storageProvider.saveMetadata(metadata2Update);

        List<JobRunrMetadata> metadataListAfterUpdate = storageProvider.getMetadata("shouldNotHappenException");
        assertThat(metadataListAfterUpdate).hasSize(2);

        // DEL
        storageProvider.deleteMetadata("shouldNotHappenException");

        // LIST
        List<JobRunrMetadata> metadataListAfterDelete = storageProvider.getMetadata("shouldNotHappenException");
        assertThat(metadataListAfterDelete).isEmpty();
    }

    @Test
    void testOnChangeListenerForSaveAndDeleteMetadata() {
        final SimpleMetadataOnChangeListener onChangeListener = new SimpleMetadataOnChangeListener();
        storageProvider.addJobStorageOnChangeListener(onChangeListener);

        JobRunrMetadata metadata = new JobRunrMetadata(onChangeListener.listenForChangesOfMetadataName(), "some-owner", "some-value");
        storageProvider.saveMetadata(metadata);
        assertThat(onChangeListener.changes).hasSize(1);
        assertThat(onChangeListener.changes.get(0)).hasSize(1);

        storageProvider.deleteMetadata(metadata.getName());
        assertThat(onChangeListener.changes).hasSizeGreaterThanOrEqualTo(2);
        assertThat(onChangeListener.changes.get(onChangeListener.changes.size() - 1)).isEmpty();
    }

    @Test
    void testCRUDJobLifeCycle() {
        // SCHEDULED
        Job scheduledJob = aScheduledJob().build();
        Job createdJob = storageProvider.save(scheduledJob);
        Job savedScheduledJob = storageProvider.getJobById(createdJob.getId());
        assertThat(savedScheduledJob).isEqualTo(createdJob);
        assertThatJobs(storageProvider.getScheduledJobs(now(), AmountBasedList.ascOnUpdatedAt(1000))).contains(createdJob);

        // ENQUEUE
        savedScheduledJob.enqueue();
        storageProvider.save(savedScheduledJob);
        Job savedEnqueuedJob = storageProvider.getJobById(createdJob.getId());
        assertThat(savedEnqueuedJob).isEqualTo(savedScheduledJob);
        assertThatJobs(storageProvider.getScheduledJobs(now(), AmountBasedList.ascOnUpdatedAt(1000))).isEmpty();
        assertThatJobs(storageProvider.getJobList(ENQUEUED, AmountBasedList.ascOnUpdatedAt(1000))).contains(savedEnqueuedJob);

        // PROCESSING
        savedEnqueuedJob.startProcessingOn(backgroundJobServer);
        storageProvider.save(savedEnqueuedJob);
        Job savedProcessingJob = storageProvider.getJobById(createdJob.getId());
        assertThat(savedProcessingJob).isEqualTo(savedEnqueuedJob);
        assertThatJobs(storageProvider.getJobList(ENQUEUED, AmountBasedList.ascOnUpdatedAt(1000))).isEmpty();
        assertThatJobs(storageProvider.getJobList(PROCESSING, AmountBasedList.ascOnUpdatedAt(1000))).contains(savedProcessingJob);

        // FAILED & RESCHEDULED
        savedProcessingJob.failed("A failure", new RuntimeException());
        savedProcessingJob.scheduleAt(now(), "Job failed");
        storageProvider.save(savedProcessingJob);
        Job savedRescheduledJob = storageProvider.getJobById(createdJob.getId());
        assertThat(savedRescheduledJob).isEqualTo(savedProcessingJob);
        assertThatJobs(storageProvider.getScheduledJobs(now(), AmountBasedList.ascOnUpdatedAt(1000))).contains(savedRescheduledJob);
        assertThatJobs(storageProvider.getJobList(PROCESSING, AmountBasedList.ascOnUpdatedAt(1000))).isEmpty();

        // ENQUEUED
        savedRescheduledJob.enqueue();
        storageProvider.save(savedRescheduledJob);
        Job savedEnqueuedJobRetry = storageProvider.getJobById(createdJob.getId());
        assertThat(savedEnqueuedJobRetry).isEqualTo(savedRescheduledJob);
        assertThatJobs(storageProvider.getScheduledJobs(now(), AmountBasedList.ascOnUpdatedAt(1000))).isEmpty();
        assertThatJobs(storageProvider.getJobList(ENQUEUED, AmountBasedList.ascOnUpdatedAt(1000))).contains(savedEnqueuedJobRetry);

        // PROCESSING
        savedEnqueuedJobRetry.startProcessingOn(backgroundJobServer);
        storageProvider.save(savedEnqueuedJobRetry);
        Job savedProcessingJobRetry = storageProvider.getJobById(createdJob.getId());
        assertThat(savedProcessingJobRetry).isEqualTo(savedEnqueuedJobRetry);
        assertThatJobs(storageProvider.getJobList(ENQUEUED, AmountBasedList.ascOnUpdatedAt(1000))).isEmpty();
        assertThatJobs(storageProvider.getJobList(PROCESSING, AmountBasedList.ascOnUpdatedAt(1000))).contains(savedProcessingJobRetry);

        // SUCCEEDED
        savedProcessingJobRetry.succeeded();
        storageProvider.save(savedProcessingJobRetry);
        Job savedSucceededJob = storageProvider.getJobById(createdJob.getId());
        assertThat(savedSucceededJob).isEqualTo(savedProcessingJobRetry);
        assertThatJobs(storageProvider.getJobList(PROCESSING, AmountBasedList.ascOnUpdatedAt(1000))).isEmpty();
        assertThatJobs(storageProvider.getJobList(SUCCEEDED, AmountBasedList.ascOnUpdatedAt(1000))).contains(savedSucceededJob);

        // DELETED
        savedSucceededJob.delete("By test");
        storageProvider.save(savedSucceededJob);
        Job fetchedDeletedJob = storageProvider.getJobById(createdJob.getId());
        assertThat(fetchedDeletedJob).hasState(DELETED);
        assertThatJobs(storageProvider.getJobList(SUCCEEDED, AmountBasedList.ascOnUpdatedAt(1000))).isEmpty();
        assertThatJobs(storageProvider.getJobList(DELETED, AmountBasedList.ascOnUpdatedAt(1000))).contains(fetchedDeletedJob);

        // DELETED PERMANENTLY
        final int permanentlyDeletedJobs = storageProvider.deletePermanently(createdJob.getId());
        assertThat(permanentlyDeletedJobs).isEqualTo(1);
        assertThatThrownBy(() -> storageProvider.getJobById(savedEnqueuedJob.getId())).isInstanceOf(JobNotFoundException.class);
        assertThatJobs(storageProvider.getJobList(DELETED, AmountBasedList.ascOnUpdatedAt(1000))).isEmpty();
    }

    @Test
    void testOptimisticLockingOnSaveJob() {
        Job job = anEnqueuedJob().build();
        Job createdJob = storageProvider.save(job);
        Job fetchedJob = storageProvider.getJobById(createdJob.getId());

        Job job1 = jobMapper.deserializeJob(jobMapper.serializeJob(fetchedJob));
        Job job2 = jobMapper.deserializeJob(jobMapper.serializeJob(fetchedJob));

        job1.startProcessingOn(backgroundJobServer);
        job2.startProcessingOn(backgroundJobServer);

        storageProvider.save(job1);
        assertThatThrownBy(() -> storageProvider.save(job2)).isInstanceOf(ConcurrentJobModificationException.class);

        assertThat(job1).hasVersion(2);
        assertThat(job2).hasVersion(1);
    }

    @Test
    void testOptimisticLockingOnSaveJobForJobThatWasDeleted() {
        Job job = anEnqueuedJob().build();
        storageProvider.save(job);
        storageProvider.deletePermanently(job.getId());
        job.succeeded();
        assertThatThrownBy(() -> storageProvider.save(job))
                .isInstanceOf(ConcurrentJobModificationException.class);
    }

    @Test
    void testSaveOfJobWithSameId() {
        UUID id = UUID.randomUUID();
        Job job1 = anEnqueuedJob().withId(id).build();
        Job job2 = anEnqueuedJob().withId(id).build();

        storageProvider.save(job1);
        assertThatThrownBy(() -> storageProvider.save(job2)).isInstanceOf(ConcurrentJobModificationException.class);
    }

    @Test
    void testExceptionOnSaveJob() {
        Job job = anEnqueuedJob().build();
        Job enqueuedJob = storageProvider.save(job);

        job.startProcessingOn(backgroundJobServer);
        try (ThrowingStorageProvider ignored = makeThrowingStorageProvider(storageProvider)) {
            assertThatThrownBy(() -> storageProvider.save(enqueuedJob))
                    .isInstanceOf(StorageException.class);
        }

        job.updateProcessing();
        assertThatCode(() -> storageProvider.save(enqueuedJob)).doesNotThrowAnyException();
    }

    @Test
    void noExceptionOnSaveOfEmptyJobs() {
        assertThatCode(() -> storageProvider.save(emptyList()))
                .doesNotThrowAnyException();
    }

    @Test
    void testOptimisticLockingOnSaveJobs() {
        Job job = aJobInProgress().build();
        Job createdJob1 = storageProvider.save(aCopyOf(job).withId().build());
        Job createdJob2 = storageProvider.save(aCopyOf(job).withId().build());
        Job createdJob3 = storageProvider.save(aCopyOf(job).withId().build());
        Job createdJob4 = storageProvider.save(aCopyOf(job).withId().build());

        storageProvider.save(aCopyOf(createdJob2).withSucceededState().build());
        storageProvider.save(aCopyOf(createdJob3).withDeletedState().build());

        createdJob1.updateProcessing();
        createdJob2.updateProcessing();
        createdJob3.updateProcessing();
        createdJob4.updateProcessing();

        assertThatThrownBy(() -> storageProvider.save(asList(createdJob1, createdJob2, createdJob3, createdJob4)))
                .isInstanceOf(ConcurrentJobModificationException.class)
                .has(failedJob(createdJob2))
                .has(failedJob(createdJob3));

        assertThat(asList(createdJob1, createdJob4)).allMatch(dbJob -> dbJob.getVersion() == 2);
        assertThat(asList(createdJob2, createdJob3)).allMatch(dbJob -> dbJob.getVersion() == 1); // as we don't know the version in the DB, we keep the original version

        assertThat(storageProvider.getJobById(createdJob1.getId())).hasVersion(2); // save all jobs failed so has version 1
        assertThat(storageProvider.getJobById(createdJob2.getId())).hasVersion(2); // already saved with succeeded state and has version 2
        assertThat(storageProvider.getJobById(createdJob3.getId())).hasVersion(2); // already saved with deleted state and has version 2
        assertThat(storageProvider.getJobById(createdJob4.getId())).hasVersion(2); // save all jobs failed so has version 1
    }

    @Test
    void testGetJobsToProcessReturnsJobInProcessingStateAndCallFilters() {
        // GIVEN
        Job scheduledJob = aScheduledJob().build();
        Job enqueuedJob1 = aJob().withEnqueuedState(now().minusSeconds(20)).build();
        Job enqueuedJob2 = aJob().withEnqueuedState(now().minusSeconds(15)).build();
        Job enqueuedJob3 = aJob().withEnqueuedState(now().minusSeconds(10)).build();
        Job enqueuedJob4 = aJob().withEnqueuedState(now().minusSeconds(5)).build();
        Job jobInProgress = aJobInProgress().build();
        Job succeededJob = aSucceededJob().build();
        Job failedJob = aFailedJob().build();
        Job deletedJob = aDeletedJob().build();
        storageProvider.save(asList(scheduledJob, enqueuedJob1, enqueuedJob2, enqueuedJob3, enqueuedJob4, jobInProgress, succeededJob, failedJob, deletedJob));

        LogAllStateChangesFilter logAllStateChangesFilter = new LogAllStateChangesFilter();
        backgroundJobServer.setJobFilters(List.of(logAllStateChangesFilter));

        // WHEN
        List<Job> jobsToProcess1 = storageProvider.getJobsToProcess(backgroundJobServer, AmountBasedList.ascOnUpdatedAt(3));

        // THEN
        assertThatJobs(jobsToProcess1)
                .hasSize(3)
                .allMatch(job -> job.hasState(PROCESSING))
                .containsExactlyComparingById(enqueuedJob1, enqueuedJob2, enqueuedJob3);
        assertThat(logAllStateChangesFilter.getAllStateChanges()).containsOnly("ENQUEUED->PROCESSING");

        // WHEN
        List<Job> jobsToProcess2 = storageProvider.getJobsToProcess(backgroundJobServer, AmountBasedList.ascOnUpdatedAt(3));

        // THEN
        assertThatJobs(jobsToProcess2)
                .hasSize(1)
                .allMatch(job -> job.hasState(PROCESSING))
                .containsExactlyComparingById(enqueuedJob4);
        assertThat(logAllStateChangesFilter.getAllStateChanges()).containsOnly("ENQUEUED->PROCESSING");
    }

    @Test
    void testGetJobsToProcessTakesStateElectionFiltersIntoAccount() {
        // GIVEN
        Job scheduledJob = aScheduledJob().build();
        Job enqueuedJob1 = aJob().withEnqueuedState(now().minusSeconds(20)).build();
        Job enqueuedJob2 = aJob().withJobDetails(TestService::tryToDoWorkButDontBecauseOfSomeBusinessRuleDefinedInTheOnStateElectionFilter).withEnqueuedState(now().minusSeconds(15)).build();
        Job enqueuedJob3 = aJob().withEnqueuedState(now().minusSeconds(10)).build();
        Job enqueuedJob4 = aJob().withEnqueuedState(now().minusSeconds(5)).build();
        Job jobInProgress = aJobInProgress().build();
        Job succeededJob = aSucceededJob().build();
        Job failedJob = aFailedJob().build();
        Job deletedJob = aDeletedJob().build();
        storageProvider.save(asList(scheduledJob, enqueuedJob1, enqueuedJob2, enqueuedJob3, enqueuedJob4, jobInProgress, succeededJob, failedJob, deletedJob));

        LogAllStateChangesFilter logAllStateChangesFilter = new LogAllStateChangesFilter();
        backgroundJobServer.setJobFilters(List.of(logAllStateChangesFilter));

        // WHEN
        List<Job> jobsToProcess = storageProvider.getJobsToProcess(backgroundJobServer, AmountBasedList.ascOnUpdatedAt(3));

        // THEN
        assertThatJobs(jobsToProcess)
                .hasSize(2)
                .allMatch(job -> job.hasState(PROCESSING))
                .containsExactlyComparingById(enqueuedJob1, enqueuedJob3);
        assertThat(logAllStateChangesFilter.getStateChanges(enqueuedJob1))
                .containsOnly("ENQUEUED->PROCESSING");
        assertThat(logAllStateChangesFilter.getStateChanges(enqueuedJob2))
                .containsOnly("ENQUEUED->PROCESSING", "PROCESSING->DELETED", "DELETED->SCHEDULED");
        assertThat(logAllStateChangesFilter.getStateChanges(enqueuedJob3))
                .containsOnly("ENQUEUED->PROCESSING");
    }

    @Test
    void testGetJobsToProcessOnExceptionReturnsJobInProcessingStateAndCallFilters() {
        // GIVEN
        UUID backgroundJobServerId = UUID.randomUUID();
        Job scheduledJob = aScheduledJob().build();
        Job enqueuedJob1 = aJob().withEnqueuedState(now().minusSeconds(20)).build();
        Job enqueuedJob2 = aJob().withEnqueuedState(now().minusSeconds(15)).build();
        Job enqueuedJob3 = aJob().withEnqueuedState(now().minusSeconds(10)).build();
        Job enqueuedJob4 = aJob().withEnqueuedState(now().minusSeconds(5)).build();
        Job jobInProgress = aJobInProgress().build();
        Job succeededJob = aSucceededJob().build();
        Job failedJob = aFailedJob().build();
        Job deletedJob = aDeletedJob().build();
        storageProvider.save(asList(scheduledJob, enqueuedJob1, enqueuedJob2, enqueuedJob3, enqueuedJob4, jobInProgress, succeededJob, failedJob, deletedJob));

        LogAllStateChangesFilter logAllStateChangesFilter = new LogAllStateChangesFilter();
        backgroundJobServer.setJobFilters(List.of(logAllStateChangesFilter));
        // simulate concurrent JobModification Exception
        when(backgroundJobServerConfiguration.getId())
                .thenReturn(backgroundJobServerId)
                .thenReturn(backgroundJobServerId)
                .thenThrow(new ConcurrentJobModificationException(enqueuedJob3));

        // WHEN
        List<Job> jobsToProcess = storageProvider.getJobsToProcess(backgroundJobServer, AmountBasedList.ascOnUpdatedAt(3));

        // THEN
        assertThatJobs(jobsToProcess)
                .hasSize(2)
                .allMatch(job -> job.hasState(PROCESSING))
                .containsExactlyComparingById(enqueuedJob1, enqueuedJob2);
        assertThat(logAllStateChangesFilter.getAllStateChanges()).containsOnly("ENQUEUED->PROCESSING");
    }

    @Test
    void testGetDistinctJobSignatures() {
        TestService testService = new TestService();
        Job job1 = aScheduledJob().withJobDetails(() -> testService.doWork(UUID.randomUUID())).build();
        Job job2 = anEnqueuedJob().withJobDetails(() -> testService.doWork(2)).build();
        Job job3 = anEnqueuedJob().withJobDetails(() -> testService.doWork(2)).build();
        Job job4 = anEnqueuedJob().withJobDetails(() -> testService.doWorkThatTakesLong(5)).build();
        Job job5 = aJobInProgress().withJobDetails(() -> testService.doWork(2, 5)).build();
        Job job6 = aSucceededJob().withJobDetails(() -> testService.doWork(UUID.randomUUID())).build();

        storageProvider.save(asList(job1, job2, job3, job4, job5, job6));

        Set<String> distinctJobSignaturesForScheduledJobs = storageProvider.getDistinctJobSignatures(SCHEDULED);
        assertThat(distinctJobSignaturesForScheduledJobs)
                .hasSize(1)
                .containsOnly("org.jobrunr.stubs.TestService.doWork(java.util.UUID)");

        Set<String> distinctJobSignaturesForEnqueuedJobs = storageProvider.getDistinctJobSignatures(ENQUEUED);
        assertThat(distinctJobSignaturesForEnqueuedJobs)
                .hasSize(2)
                .containsOnly(
                        "org.jobrunr.stubs.TestService.doWorkThatTakesLong(java.lang.Integer)",
                        "org.jobrunr.stubs.TestService.doWork(java.lang.Integer)");

        Set<String> distinctJobSignaturesForJobsInProgress = storageProvider.getDistinctJobSignatures(PROCESSING);
        assertThat(distinctJobSignaturesForJobsInProgress)
                .hasSize(1)
                .containsOnly(
                        "org.jobrunr.stubs.TestService.doWork(java.lang.Integer,java.lang.Integer)");

        Set<String> distinctJobSignaturesForSucceededJobs = storageProvider.getDistinctJobSignatures(SUCCEEDED);
        assertThat(distinctJobSignaturesForSucceededJobs)
                .hasSize(1)
                .containsOnly(
                        "org.jobrunr.stubs.TestService.doWork(java.util.UUID)");

        Set<String> distinctJobSignaturesForScheduledAndEnqueuedJobs = storageProvider.getDistinctJobSignatures(SCHEDULED, ENQUEUED);
        assertThat(distinctJobSignaturesForScheduledAndEnqueuedJobs)
                .hasSize(3)
                .containsOnly(
                        "org.jobrunr.stubs.TestService.doWork(java.util.UUID)",
                        "org.jobrunr.stubs.TestService.doWorkThatTakesLong(java.lang.Integer)",
                        "org.jobrunr.stubs.TestService.doWork(java.lang.Integer)");
    }

    @Test
    void testGetRecurringJobScheduledInstants() {
        JobDetails jobDetails = defaultJobDetails().build();
        RecurringJob recurringJob = aDefaultRecurringJob().withJobDetails(jobDetails).build();
        Job scheduledJob = recurringJob.toScheduledJobs(now(), now().plusSeconds(15)).get(0);
        Instant scheduledAt = ((ScheduledState) scheduledJob.getJobState()).getScheduledAt();

        storageProvider.save(scheduledJob);
        assertThat(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId())).isEqualTo(List.of(scheduledAt));
        assertThat(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED)).isEqualTo(List.of(scheduledAt));
        assertThat(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED)).isEqualTo(List.of(scheduledAt));
        assertThat(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), ENQUEUED, PROCESSING, SUCCEEDED)).isEmpty();

        scheduledJob.enqueue();
        storageProvider.save(scheduledJob);
        assertThat(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED)).isEqualTo(List.of(scheduledAt));
        assertThat(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), ENQUEUED)).isEqualTo(List.of(scheduledAt));
        assertThat(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, PROCESSING, SUCCEEDED)).isEmpty();

        scheduledJob.delete("For test");
        storageProvider.save(scheduledJob);
        assertThat(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, PROCESSING, SUCCEEDED)).isEmpty();
        assertThat(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), ENQUEUED, DELETED)).isEqualTo(List.of(scheduledAt));

        storageProvider.deletePermanently(scheduledJob.getId());
        assertThat(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId())).isEmpty();
    }

    @Test
    void testSaveListUpdateListAndGetListOfJobs() {
        final List<Job> jobs = asList(
                aJob().withName("1").withEnqueuedState(now().minusSeconds(30)).build(),
                aJob().withName("2").withEnqueuedState(now().minusSeconds(20)).build(),
                aJob().withName("3").withEnqueuedState(now().minusSeconds(10)).build());
        final List<Job> savedJobs = storageProvider.save(jobs);

        assertThat(storageProvider).hasJobs(ENQUEUED, 3);
        assertThat(storageProvider).hasJobs(PROCESSING, 0);

        savedJobs.forEach(job -> {
            job.startProcessingOn(backgroundJobServer);
            sleep(100);
        });
        storageProvider.save(savedJobs);

        assertThat(storageProvider).hasJobs(ENQUEUED, 0);
        assertThat(storageProvider).hasJobs(PROCESSING, 3);

        List<Job> fetchedJobsAsc = storageProvider.getJobList(PROCESSING, AmountBasedList.ascOnUpdatedAt(100));
        assertThatJobs(fetchedJobsAsc)
                .hasSize(3)
                .containsAll(savedJobs);
        assertThat(fetchedJobsAsc).extracting("jobName").containsExactly("1", "2", "3");

        List<Job> fetchedJobsDesc = storageProvider.getJobList(PROCESSING, AmountBasedList.descOnUpdatedAt(100));
        assertThatJobs(fetchedJobsDesc)
                .hasSize(3)
                .containsAll(savedJobs);
        assertThat(fetchedJobsDesc).extracting("jobName").containsExactly("3", "2", "1");
    }


    @Test
    void testExceptionOnSaveListOfJobs() {
        final List<Job> jobs = asList(
                aJob().withName("1").withEnqueuedState(now().minusSeconds(30)).build(),
                aJob().withName("2").withEnqueuedState(now().minusSeconds(20)).build(),
                aJob().withName("3").withEnqueuedState(now().minusSeconds(10)).build());
        final List<Job> savedJobs = storageProvider.save(jobs);
        savedJobs.forEach(job -> job.startProcessingOn(backgroundJobServer));

        try (ThrowingStorageProvider ignored = makeThrowingStorageProvider(storageProvider)) {
            assertThatThrownBy(() -> storageProvider.save(savedJobs))
                    .isInstanceOf(StorageException.class);
        }

        savedJobs.forEach(Job::updateProcessing);
        storageProvider.save(savedJobs);
    }

    @Test
    void testJobPageCanBeSorted() {
        final List<Job> jobs = asList(
                aJob().withEnqueuedState(now().minusSeconds(10)).build(),
                aJob().withEnqueuedState(now().minusSeconds(8)).build(),
                aJob().withEnqueuedState(now().minusSeconds(6)).build(),
                aJob().withEnqueuedState(now().minusSeconds(4)).build(),
                aJob().withEnqueuedState(now().minusSeconds(2)).build()
        );

        storageProvider.save(jobs);

        Page<Job> fetchedJobsAscOnPriorityAndAscOnCreated = storageProvider.getJobs(ENQUEUED, OffsetBasedPage.ascOnUpdatedAt(50));
        assertThatJobs(fetchedJobsAscOnPriorityAndAscOnCreated.getItems())
                .hasSize(5)
                .containsExactly(jobs.get(0), jobs.get(1), jobs.get(2), jobs.get(3), jobs.get(4));

        Page<Job> fetchedJobsDescOnUpdatedAt = storageProvider.getJobs(ENQUEUED, OffsetBasedPage.descOnUpdatedAt(50));
        assertThatJobs(fetchedJobsDescOnUpdatedAt.getItems())
                .hasSize(5)
                .containsExactly(jobs.get(4), jobs.get(3), jobs.get(2), jobs.get(1), jobs.get(0));
    }

    @Test
    void testJobPageCanUseOffsetAndLimit() {
        final List<Job> jobs = asList(
                aJob().withEnqueuedState(now().minusSeconds(10)).build(),
                aJob().withEnqueuedState(now().minusSeconds(8)).build(),
                aJob().withEnqueuedState(now().minusSeconds(6)).build(),
                aJob().withEnqueuedState(now().minusSeconds(4)).build(),
                aJob().withEnqueuedState(now().minusSeconds(2)).build()
        );

        storageProvider.save(jobs);

        Page<Job> fetchedJobsAsc = storageProvider.getJobs(ENQUEUED, OffsetBasedPage.ascOnUpdatedAt(2, 2));
        assertThatJobs(fetchedJobsAsc.getItems())
                .hasSize(2)
                .containsExactly(jobs.get(2), jobs.get(3));

        Page<Job> fetchedJobsDesc = storageProvider.getJobs(ENQUEUED, OffsetBasedPage.descOnUpdatedAt(2, 2));
        assertThatJobs(fetchedJobsDesc.getItems())
                .hasSize(2)
                .containsExactly(jobs.get(2), jobs.get(1));
    }

    @Test
    void testGetListOfJobsUpdatedBefore() {
        final List<Job> jobs = asList(
                aJob().withEnqueuedState(now().minus(24, HOURS)).build(),
                aJob().withEnqueuedState(now().minus(12, HOURS)).build(),
                aJob().withEnqueuedState(now().minus(2, HOURS)).build(),
                aJob().withEnqueuedState(now()).build()
        );
        storageProvider.save(jobs);

        assertThatJobs(storageProvider.getJobList(ENQUEUED, now().minus(3, HOURS), AmountBasedList.ascOnUpdatedAt(100)))
                .hasSize(2)
                .containsExactly(jobs.get(0), jobs.get(1));

        assertThatJobs(storageProvider.getJobList(ENQUEUED, now().minus(1, HOURS), AmountBasedList.ascOnUpdatedAt(100)))
                .hasSize(3)
                .containsExactly(jobs.get(0), jobs.get(1), jobs.get(2));

        assertThatJobs(storageProvider.getJobList(ENQUEUED, now().minus(1, HOURS), AmountBasedList.descOnUpdatedAt(100)))
                .hasSize(3)
                .containsExactly(jobs.get(2), jobs.get(1), jobs.get(0));

        assertThatJobs(storageProvider.getJobList(PROCESSING, now().minus(1, HOURS), AmountBasedList.ascOnUpdatedAt(100)))
                .isEmpty();
    }

    @Test
    void testDeleteJobs() {
        final List<Job> jobs = asList(
                aJob().withEnqueuedState(now().minus(4, HOURS)).build(),
                aJob().withEnqueuedState(now().minus(3, HOURS)).build(),
                aJob().withEnqueuedState(now().minus(2, HOURS)).build(),
                aJob().withEnqueuedState(now()).build()
        );

        storageProvider.save(jobs);

        storageProvider.deleteJobsPermanently(ENQUEUED, now().minus(1, HOURS));

        List<Job> fetchedJobs = storageProvider.getJobList(ENQUEUED, AmountBasedList.ascOnUpdatedAt(100));

        assertThat(fetchedJobs).hasSize(1);
    }

    @Test
    void testGetCarbonAwareJobsList() {
        final List<Job> jobs = storageProvider.save(asList(
                aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(4, HOURS))).build(),
                aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(12, HOURS))).build(),
                aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(36, HOURS))).build(),
                aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(48, HOURS))).build()
        ));

        assertThatJobs(storageProvider.getCarbonAwareJobList(now().plus(3, HOURS), AmountBasedList.ascOnScheduledAt(100)))
                .hasSize(0);
        assertThatJobs(storageProvider.getCarbonAwareJobList(now().plus(5, HOURS), AmountBasedList.ascOnScheduledAt(100)))
                .hasSize(1)
                .containsExactly(jobs.get(0));
        assertThatJobs(storageProvider.getCarbonAwareJobList(now().plus(50, HOURS), AmountBasedList.ascOnScheduledAt(100)))
                .hasSize(4)
                .containsExactly(jobs.get(0), jobs.get(1), jobs.get(2), jobs.get(3));
        assertThatJobs(storageProvider.getCarbonAwareJobList(now().plus(50, HOURS), AmountBasedList.ascOnScheduledAt(1)))
                .hasSize(1)
                .containsExactly(jobs.get(0));

        Job existingCarbonAwareJob = jobs.getFirst();
        existingCarbonAwareJob.scheduleAt(Instant.now(), "test");
        storageProvider.save(existingCarbonAwareJob);

        assertThatJobs(storageProvider.getCarbonAwareJobList(now().plus(50, HOURS), AmountBasedList.ascOnScheduledAt(100)))
                .hasSize(3)
                .containsExactly(jobs.get(1), jobs.get(2), jobs.get(3));
    }

    @Test
    void testScheduledJobs() {
        Job job1 = anEnqueuedJob().withState(new ScheduledState(now())).build();
        Job job2 = anEnqueuedJob().withState(new ScheduledState(now().plus(20, HOURS))).build();
        final List<Job> jobs = asList(job1, job2);

        storageProvider.save(jobs);

        assertThatJobs(storageProvider.getScheduledJobs(now().plus(5, SECONDS), AmountBasedList.ascOnUpdatedAt(100)))
                .hasSize(1)
                .contains(job1);
    }

    @Test
    void testScheduledJobsPage() {
        Job job1 = anEnqueuedJob().withState(new ScheduledState(now())).build();
        Job job2 = anEnqueuedJob().withState(new ScheduledState(now().plusSeconds(1))).build();
        Job job3 = anEnqueuedJob().withState(new ScheduledState(now().plusSeconds(2))).build();
        final List<Job> jobs = asList(job1, job2, job3);

        storageProvider.save(jobs);

        Page<Job> jobPage1 = storageProvider.getScheduledJobs(now().plus(5, SECONDS), OffsetBasedPage.ascOnUpdatedAt(2));

        assertThatJobs(jobPage1.getItems())
                .hasSize(2)
                .contains(job1, job2);

        Page<Job> jobPage2 = storageProvider.getScheduledJobs(now().plus(5, SECONDS), OffsetBasedPageRequest.fromString(jobPage1.getNextPageRequest()));

        assertThatJobs(jobPage2.getItems())
                .hasSize(1)
                .contains(job3);
    }

    @Test
    void testCRUDRecurringJobLifeCycle() {
        assertThat(storageProvider.recurringJobsUpdated(0L)).isFalse();

        RecurringJob recurringJobv1 = aDefaultRecurringJob().withId("my-job").withCronExpression(Cron.daily()).build();
        storageProvider.saveRecurringJob(recurringJobv1);
        assertThat(storageProvider.recurringJobsUpdated(0L)).isTrue();
        RecurringJobsResult recurringJobsResult1 = storageProvider.getRecurringJobs();
        assertThat(recurringJobsResult1).hasSize(1);
        await().untilAsserted(() -> assertThat(storageProvider.recurringJobsUpdated(recurringJobsResult1.getLastModifiedHash())).isFalse());


        RecurringJob recurringJobv2 = aDefaultRecurringJob().withId("my-job").withCronExpression(Cron.hourly()).build();
        storageProvider.saveRecurringJob(recurringJobv2);

        await().untilAsserted(() -> assertThat(storageProvider.recurringJobsUpdated(recurringJobsResult1.getLastModifiedHash())).isTrue());
        RecurringJobsResult recurringJobsResult2 = storageProvider.getRecurringJobs();
        assertThat(recurringJobsResult2).hasSize(1);
        await().untilAsserted(() -> assertThat(storageProvider.recurringJobsUpdated(recurringJobsResult2.getLastModifiedHash())).isFalse());

        assertThat(storageProvider.getRecurringJobs().get(0).getScheduleExpression()).isEqualTo(Cron.hourly());

        RecurringJob otherRecurringJob = aDefaultRecurringJob().withId("my-other-job").withCronExpression(Cron.hourly()).build();
        storageProvider.saveRecurringJob(otherRecurringJob);
        await().untilAsserted(() -> assertThat(storageProvider.recurringJobsUpdated(recurringJobsResult2.getLastModifiedHash())).isTrue());
        RecurringJobsResult recurringJobsResult3 = storageProvider.getRecurringJobs();
        assertThat(recurringJobsResult3).hasSize(2);
        await().untilAsserted(() -> assertThat(storageProvider.recurringJobsUpdated(recurringJobsResult3.getLastModifiedHash())).isFalse());

        int deleted = storageProvider.deleteRecurringJob("my-job");
        assertThat(deleted).isEqualTo(1);
        await().untilAsserted(() -> assertThat(storageProvider.recurringJobsUpdated(recurringJobsResult3.getLastModifiedHash())).isTrue());
        RecurringJobsResult recurringJobsResult4 = storageProvider.getRecurringJobs();
        assertThat(recurringJobsResult4).hasSize(1);
        await().untilAsserted(() -> assertThat(storageProvider.recurringJobsUpdated(recurringJobsResult4.getLastModifiedHash())).isFalse());

        // DELETE NON EXISTENT RECURRING JOB
        int deletedForNonExistingJob = storageProvider.deleteRecurringJob("non-existing-recurring-job");
        assertThat(deletedForNonExistingJob).isEqualTo(0);
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    void testOnChangeListenerForSaveAndDeleteJob() {
        final SimpleJobStorageOnChangeListener onChangeListener = new SimpleJobStorageOnChangeListener();
        storageProvider.addJobStorageOnChangeListener(onChangeListener);

        Job job = anEnqueuedJob().build();
        storageProvider.save(job);
        await().untilAsserted(() -> assertThat(onChangeListener.changes).hasSize(1));

        job.delete("For test");
        storageProvider.save(job);
        await().untilAsserted(() -> assertThat(onChangeListener.changes).hasSize(2));
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    void testOnChangeListenerForSaveJobList() {
        final SimpleJobStorageOnChangeListener onChangeListener = new SimpleJobStorageOnChangeListener();
        storageProvider.addJobStorageOnChangeListener(onChangeListener);

        final List<Job> jobs = asList(anEnqueuedJob().build(), anEnqueuedJob().build());
        storageProvider.save(jobs);
        await().untilAsserted(() -> assertThat(onChangeListener.changes).hasSize(1));

        jobs.forEach(job -> job.startProcessingOn(backgroundJobServer));
        storageProvider.save(jobs);
        await().untilAsserted(() -> assertThat(onChangeListener.changes).hasSize(2));
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    void testOnChangeListenerForDeleteJobsByState() {
        storageProvider.save(asList(anEnqueuedJob().build(), anEnqueuedJob().build()));

        final SimpleJobStorageOnChangeListener onChangeListener = new SimpleJobStorageOnChangeListener();
        storageProvider.addJobStorageOnChangeListener(onChangeListener);

        storageProvider.deleteJobsPermanently(ENQUEUED, now());

        await().untilAsserted(() -> assertThat(onChangeListener.changes).hasSize(1));
    }

    @Test
    void testJobStats() {
        storageProvider.announceBackgroundJobServer(backgroundJobServer.getServerStatus());

        assertThatCode(() -> storageProvider.getJobStats()).doesNotThrowAnyException();

        storageProvider.publishTotalAmountOfSucceededJobs(5);
        storageProvider.save(asList(
                aCarbonAwaitingJob().build(),
                anEnqueuedJob().build(),
                anEnqueuedJob().build(),
                anEnqueuedJob().build(),
                aJobInProgress().build(),
                aScheduledJob().build(),
                aFailedJob().build(),
                aFailedJob().build(),
                aSucceededJob().build(),
                aDeletedJob().build()
        ));
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("id1").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("id2").build());

        final JobStats jobStats = storageProvider.getJobStats();
        assertThat(jobStats.getAwaiting()).isEqualTo(1);
        assertThat(jobStats.getScheduled()).isEqualTo(1);
        assertThat(jobStats.getEnqueued()).isEqualTo(3);
        assertThat(jobStats.getProcessing()).isEqualTo(1);
        assertThat(jobStats.getFailed()).isEqualTo(2);
        assertThat(jobStats.getSucceeded()).isEqualTo(1);
        assertThat(jobStats.getAllTimeSucceeded()).isEqualTo(5);
        assertThat(jobStats.getDeleted()).isEqualTo(1);
        assertThat(jobStats.getRecurringJobs()).isEqualTo(2);
        assertThat(jobStats.getBackgroundJobServers()).isEqualTo(1);
    }


    @Test
    @Disabled
    void testPerformance() {
        int amount = 1000000;
        IntStream.range(0, amount)
                .peek(i -> {
                    if (i % 10000 == 0) {
                        System.out.println("Saving job " + i);
                    }
                })
                .mapToObj(i -> anEnqueuedJob().withJobDetails(systemOutPrintLnJobDetails("this is test " + i)).build())
                .collect(batchCollector(1000, storageProvider::save));

        AtomicInteger atomicInteger = new AtomicInteger();
        storageProvider
                .getJobList(ENQUEUED, AmountBasedList.ascOnUpdatedAt(10000))
                .stream()
                .parallel()
                .peek(job -> {
                    job.startProcessingOn(backgroundJobServer);
                    storageProvider.save(job);
                    if (atomicInteger.get() % 100 == 0) {
                        System.out.println("Retrieved job " + atomicInteger.get());
                    }
                })
                .forEach(job -> atomicInteger.incrementAndGet());

        assertThat(atomicInteger).hasValue(10000);
    }

    private static class SimpleJobStorageOnChangeListener implements JobStatsChangeListener {

        private final List<JobStats> changes = new ArrayList<>();

        @Override
        public void onChange(JobStats jobStats) {
            this.changes.add(jobStats);
        }
    }

    private static class SimpleMetadataOnChangeListener implements MetadataChangeListener {

        private final List<List<JobRunrMetadata>> changes = new ArrayList<>();

        @Override
        public String listenForChangesOfMetadataName() {
            return "metadata-name";
        }

        @Override
        public void onChange(List<JobRunrMetadata> metadata) {
            this.changes.add(metadata);
        }
    }

    public static abstract class ThrowingStorageProvider implements AutoCloseable {

        private final StorageProvider storageProvider;
        private final String fieldNameForReset;
        private Object originalState;

        public ThrowingStorageProvider(StorageProvider storageProvider, String fieldNameForReset) {
            this.storageProvider = storageProvider;
            this.fieldNameForReset = fieldNameForReset;

            try {
                saveInternalStorageProviderState(storageProvider);
                makeStorageProviderThrowException(storageProvider);
            } catch (Exception e) {
                throw new RuntimeException("Exception setting up ThrowingStorageProvider", e);
            }
        }

        public void close() {
            resetStorageProviderUsingInternalState(storageProvider);
        }

        protected void saveInternalStorageProviderState(StorageProvider storageProvider) {
            this.originalState = getInternalState(storageProvider, fieldNameForReset);
        }

        protected abstract void makeStorageProviderThrowException(StorageProvider storageProvider) throws Exception;

        protected void resetStorageProviderUsingInternalState(StorageProvider storageProvider) {
            setInternalState(storageProvider, fieldNameForReset, originalState);
        }
    }
}
