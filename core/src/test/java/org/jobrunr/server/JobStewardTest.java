package org.jobrunr.server;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.SevereJobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.stubs.Mocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.JobTestBuilder.emptyJobList;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobStewardTest {

    private BackgroundJobServer backgroundJobServer = Mocks.ofBackgroundJobServer();
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private WorkDistributionStrategy workDistributionStrategy;
    @Captor
    private ArgumentCaptor<JobRunrMetadata> jobRunrMetadataArgumentCaptor;

    private BackgroundJobServerStatus backgroundJobServerStatus;
    private JobSteward jobSteward;
    private LogAllStateChangesFilter logAllStateChangesFilter;
    private ListAppender<ILoggingEvent> logger;

    @BeforeEach
    void setUpBackgroundJobZooKeeper() {
        logAllStateChangesFilter = new LogAllStateChangesFilter();
        backgroundJobServerStatus = aDefaultBackgroundJobServerStatus().withIsStarted().build();
        jobSteward = initializeJobSteward();
        logger = LoggerAssert.initFor(jobSteward);
    }

    @Test
    void jobZooKeeperDoesNothingIfItIsNotInitialized() {
        reset(storageProvider);
        when(backgroundJobServer.isUnAnnounced()).thenReturn(true);

        jobSteward.run();

        verifyNoInteractions(storageProvider);
    }

    @Test
    void evenWhenNoWorkCanBeOnboardedJobsThatAreProcessedAreBeingUpdatedWithAHeartbeat() {
        backgroundJobServerStatus = aDefaultBackgroundJobServerStatus().withWorkerSize(0).build();
        jobSteward = initializeJobSteward();

        final Job job = anEnqueuedJob().withId().build();
        lenient().when(storageProvider.getJobList(eq(ENQUEUED), any())).thenReturn(singletonList(job));

        job.startProcessingOn(backgroundJobServer);
        jobSteward.startProcessing(job, mock(Thread.class));
        jobSteward.run();
        jobSteward.startProcessing(aJobInProgress().build(), mock(Thread.class));

        verify(storageProvider).save(singletonList(job));
        ProcessingState processingState = job.getJobState();
        assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
    }

    @Test
    void checkForEnqueuedJobsIfJobsPresentSubmitsThemToTheBackgroundJobServer() {
        final Job enqueuedJob = anEnqueuedJob().build();
        final List<Job> jobs = List.of(enqueuedJob);

        lenient().when(storageProvider.getJobsToProcess(eq(backgroundJobServer), any())).thenReturn(jobs);

        jobSteward.run();

        verify(backgroundJobServer).processJob(enqueuedJob);
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnSuccess() {
        Job job = aScheduledJob().build();

        when(storageProvider.getScheduledJobs(any(Instant.class), any(AmountRequest.class)))
                .thenReturn(
                        singletonList(job),
                        emptyJobList()
                );

        jobSteward.run();

        assertThat(logAllStateChangesFilter.getStateChanges(job)).containsExactly("SCHEDULED->ENQUEUED");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(job)).isFalse();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(job)).isFalse();
    }

    @Test
    void ifNoStateChangeHappensStateChangeFiltersAreNotInvoked() {
        Job aJobInProgress = aJobInProgress().build();

        jobSteward.startProcessing(aJobInProgress, mock(Thread.class));

        for (int i = 0; i <= 5; i++) {
            jobSteward.run();
        }

        assertThat(logAllStateChangesFilter.getStateChanges(aJobInProgress)).isEmpty();
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(aJobInProgress)).isFalse();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(aJobInProgress)).isFalse();
    }

    @Test
    void severeJobRunrExceptionsAreLoggedToStorageProvider() {
        Job succeededJob1 = aSucceededJob().build();
        Job succeededJob2 = aSucceededJob().build();

        when(storageProvider.getJobById(succeededJob1.getId())).thenReturn(succeededJob1);
        when(storageProvider.getJobById(succeededJob2.getId())).thenReturn(succeededJob2);
        lenient().when(storageProvider.getJobList(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        asList(succeededJob1, succeededJob2, aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build()),
                        emptyJobList()
                );
        when(storageProvider.save(anyList())).thenThrow(new ConcurrentJobModificationException(asList(succeededJob1, succeededJob2)));

        jobSteward.run();

        verify(storageProvider).saveMetadata(jobRunrMetadataArgumentCaptor.capture());

        assertThat(jobRunrMetadataArgumentCaptor.getValue())
                .hasName(SevereJobRunrException.class.getSimpleName())
                .hasOwner("BackgroundJobServer " + backgroundJobServer.getId())
                .valueContains("## Runtime information");
    }

    @Test
    void jobZooKeeperStopsIfTooManyExceptions() {
        Job succeededJob1 = aSucceededJob().build();
        Job succeededJob2 = aSucceededJob().build();

        when(storageProvider.getJobById(succeededJob1.getId())).thenReturn(succeededJob1);
        when(storageProvider.getJobById(succeededJob2.getId())).thenReturn(succeededJob2);
        lenient().when(storageProvider.getJobList(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        asList(succeededJob1, succeededJob2, aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build())
                );
        when(storageProvider.save(anyList())).thenThrow(new ConcurrentJobModificationException(asList(succeededJob1, succeededJob2)));

        for (int i = 0; i <= 5; i++) {
            jobSteward.run();
        }

        verify(backgroundJobServer).stop();
        assertThat(logger).hasErrorMessage("FATAL - JobRunr encountered too many processing exceptions. Shutting down.");
    }

    @Test
    void jobZooKeeperStopsAndLogsStorageProviderExceptionIfTooManyStorageExceptions() {
        Job aJobInProgress = aJobInProgress().build();

        jobSteward.startProcessing(aJobInProgress, mock(Thread.class));
        when(storageProvider.save(anyList())).thenThrow(new StorageException("a storage exception"));

        for (int i = 0; i <= 5; i++) {
            jobSteward.run();
        }

        verify(backgroundJobServer).stop();
        assertThat(logger).hasErrorMessage("FATAL - JobRunr encountered too many storage exceptions. Shutting down. Did you know JobRunr Pro has built-in database fault tolerance? Check out https://www.jobrunr.io/en/documentation/pro/database-fault-tolerance/");
    }

    private JobSteward initializeJobSteward() {
        UUID backgroundJobServerId = UUID.randomUUID();
        lenient().when(backgroundJobServer.getId()).thenReturn(backgroundJobServerId);
        lenient().when(backgroundJobServer.getServerStatus()).thenReturn(backgroundJobServerStatus);
        lenient().when(backgroundJobServer.isRunning()).thenReturn(true);
        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(backgroundJobServer.getWorkDistributionStrategy()).thenReturn(workDistributionStrategy);
        when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters(logAllStateChangesFilter));
        when(backgroundJobServer.getDashboardNotificationManager()).thenReturn(new DashboardNotificationManager(backgroundJobServerId, storageProvider));
        lenient().when(workDistributionStrategy.canOnboardNewWork()).thenReturn(true);
        lenient().when(workDistributionStrategy.getWorkPageRequest()).thenReturn(ascOnUpdatedAt(10));
        lenient().when(backgroundJobServer.isAnnounced()).thenReturn(true);
        lenient().when(backgroundJobServer.isMaster()).thenReturn(true);
        return new JobSteward(backgroundJobServer);
    }
}