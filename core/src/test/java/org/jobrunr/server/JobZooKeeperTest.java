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
import org.jobrunr.storage.*;
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
import static org.jobrunr.jobs.JobTestBuilder.*;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobZooKeeperTest {

    @Mock
    private StorageProvider storageProvider;
    @Mock
    private BackgroundJobServer backgroundJobServer;
    @Mock
    private WorkDistributionStrategy workDistributionStrategy;
    @Captor
    private ArgumentCaptor<JobRunrMetadata> jobRunrMetadataArgumentCaptor;

    private BackgroundJobServerStatus backgroundJobServerStatus;
    private JobZooKeeper jobZooKeeper;
    private LogAllStateChangesFilter logAllStateChangesFilter;
    private ListAppender<ILoggingEvent> logger;

    @BeforeEach
    void setUpBackgroundJobZooKeeper() {
        BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();
        when(backgroundJobServer.getConfiguration()).thenReturn(backgroundJobServerConfiguration);
        logAllStateChangesFilter = new LogAllStateChangesFilter();
        backgroundJobServerStatus = aDefaultBackgroundJobServerStatus().withIsStarted().build();
        jobZooKeeper = initializeJobZooKeeper();
        lenient().when(backgroundJobServer.getConcurrentJobModificationResolver())
                .thenReturn(
                        backgroundJobServerConfiguration
                                .getConcurrentJobModificationPolicy()
                                .toConcurrentJobModificationResolver(storageProvider, jobZooKeeper)
                );
        logger = LoggerAssert.initFor(jobZooKeeper);
    }

    @Test
    void jobZooKeeperDoesNothingIfItIsNotInitialized() {
        when(backgroundJobServer.isUnAnnounced()).thenReturn(true);

        jobZooKeeper.run();

        verifyNoInteractions(storageProvider);
    }

    @Test
    void evenWhenNoWorkCanBeOnboardedJobsThatAreProcessedAreBeingUpdatedWithAHeartbeat() {
        backgroundJobServerStatus = aDefaultBackgroundJobServerStatus().withWorkerSize(0).build();
        jobZooKeeper = initializeJobZooKeeper();

        final Job job = anEnqueuedJob().withId().build();
        lenient().when(storageProvider.getJobs(eq(ENQUEUED), any())).thenReturn(singletonList(job));

        job.startProcessingOn(backgroundJobServer);
        jobZooKeeper.startProcessing(job, mock(Thread.class));
        jobZooKeeper.run();
        jobZooKeeper.startProcessing(aJobInProgress().build(), mock(Thread.class));

        verify(storageProvider).save(singletonList(job));
        ProcessingState processingState = job.getJobState();
        assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
    }

    @Test
    void checkForEnqueuedJobsIfJobsPresentSubmitsThemToTheBackgroundJobServer() {
        final Job enqueuedJob = anEnqueuedJob().build();
        final List<Job> jobs = List.of(enqueuedJob);

        lenient().when(storageProvider.getJobs(eq(ENQUEUED), any())).thenReturn(jobs);

        jobZooKeeper.run();

        verify(backgroundJobServer).processJob(enqueuedJob);
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnSuccess() {
        Job job = aScheduledJob().build();

        when(storageProvider.getScheduledJobs(any(Instant.class), any()))
                .thenReturn(
                        singletonList(job),
                        emptyJobList()
                );

        jobZooKeeper.run();

        assertThat(logAllStateChangesFilter.getStateChanges(job)).containsExactly("SCHEDULED->ENQUEUED");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(job)).isFalse();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(job)).isFalse();
    }

    @Test
    void ifNoStateChangeHappensStateChangeFiltersAreNotInvoked() {
        Job aJobInProgress = aJobInProgress().build();

        jobZooKeeper.startProcessing(aJobInProgress, mock(Thread.class));

        for (int i = 0; i <= 5; i++) {
            jobZooKeeper.run();
        }

        assertThat(logAllStateChangesFilter.getStateChanges(aJobInProgress)).isEmpty();
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(aJobInProgress)).isFalse();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(aJobInProgress)).isFalse();
    }

    @Test
    void severeJobRunrExceptionsAreLoggedToStorageProvider() {
        Job aJobInProgress1 = aJobInProgress().build();
        Job aJobInProgress2 = aJobInProgress().build();

        when(storageProvider.getJobById(aJobInProgress1.getId())).thenReturn(aJobInProgress1);
        when(storageProvider.getJobById(aJobInProgress2.getId())).thenReturn(aJobInProgress2);
        lenient().when(storageProvider.getJobs(eq(PROCESSING), any(Instant.class), any()))
                .thenReturn(
                        asList(aJobInProgress1, aJobInProgress2, aJobInProgress().build(), aJobInProgress().build(), aJobInProgress().build()),
                        emptyJobList()
                );
        when(storageProvider.save(anyList())).thenThrow(new ConcurrentJobModificationException(asList(aJobInProgress1, aJobInProgress2)));

        jobZooKeeper.run();

        verify(storageProvider).saveMetadata(jobRunrMetadataArgumentCaptor.capture());

        assertThat(jobRunrMetadataArgumentCaptor.getValue())
                .hasName(SevereJobRunrException.class.getSimpleName())
                .hasOwner("BackgroundJobServer " + backgroundJobServer.getId())
                .valueContains("## Runtime information");
    }

    @Test
    void jobZooKeeperStopsIfTooManyExceptions() {
        Job aJobInProgress1 = aJobInProgress().build();
        Job aJobInProgress2 = aJobInProgress().build();

        when(storageProvider.getJobById(aJobInProgress1.getId())).thenReturn(aJobInProgress1);
        when(storageProvider.getJobById(aJobInProgress2.getId())).thenReturn(aJobInProgress2);
        lenient().when(storageProvider.getJobs(eq(PROCESSING), any(Instant.class), any()))
                .thenReturn(
                        asList(aJobInProgress1, aJobInProgress2, aJobInProgress().build(), aJobInProgress().build(), aJobInProgress().build())
                );
        when(storageProvider.save(anyList())).thenThrow(new ConcurrentJobModificationException(asList(aJobInProgress1, aJobInProgress2)));

        for (int i = 0; i <= 5; i++) {
            jobZooKeeper.run();
        }

        verify(backgroundJobServer).stop();
        assertThat(logger).hasErrorMessage("FATAL - JobRunr encountered too many processing exceptions. Shutting down.");
    }

    @Test
    void jobZooKeeperStopsAndLogsStorageProviderExceptionIfTooManyStorageExceptions() {
        Job aJobInProgress = aJobInProgress().build();

        jobZooKeeper.startProcessing(aJobInProgress, mock(Thread.class));
        when(storageProvider.save(anyList())).thenThrow(new StorageException("a storage exception"));

        for (int i = 0; i <= 5; i++) {
            jobZooKeeper.run();
        }

        verify(backgroundJobServer).stop();
        assertThat(logger).hasErrorMessage("FATAL - JobRunr encountered too many storage exceptions. Shutting down. Did you know JobRunr Pro has built-in database fault tolerance? Check out https://www.jobrunr.io/en/documentation/pro/database-fault-tolerance/");
    }

    private JobZooKeeper initializeJobZooKeeper() {
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
        return new JobZooKeeper(backgroundJobServer);
    }
}