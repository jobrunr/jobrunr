package org.jobrunr.server;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.*;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
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
    private BackgroundJobTestFilter logAllStateChangesFilter;

    @BeforeEach
    void setUpBackgroundJobZooKeeper() {
        when(backgroundJobServer.getConfiguration()).thenReturn(usingStandardBackgroundJobServerConfiguration());
        logAllStateChangesFilter = new BackgroundJobTestFilter();
        backgroundJobServerStatus = aDefaultBackgroundJobServerStatus().withIsStarted().build();
        jobZooKeeper = initializeJobZooKeeper();
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
    void allStateChangesArePassingViaTheApplyStateFilterOnSuccess() {
        Job job = aScheduledJob().build();

        when(storageProvider.getScheduledJobs(any(Instant.class), any()))
                .thenReturn(
                        singletonList(job),
                        emptyJobList()
                );

        jobZooKeeper.run();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("SCHEDULED->ENQUEUED");
        assertThat(logAllStateChangesFilter.processingPassed).isFalse();
        assertThat(logAllStateChangesFilter.processedPassed).isFalse();
    }

    @Test
    void severeJobRunrExceptionsAreLoggedToStorageProvider() {
        Job succeededJob1 = aSucceededJob().build();
        Job succeededJob2 = aSucceededJob().build();

        when(storageProvider.getJobById(succeededJob1.getId())).thenReturn(succeededJob1);
        when(storageProvider.getJobById(succeededJob2.getId())).thenReturn(succeededJob2);
        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        asList(succeededJob1, succeededJob2, aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build()),
                        emptyJobList()
                );
        when(storageProvider.save(anyList())).thenThrow(new ConcurrentJobModificationException(asList(succeededJob1, succeededJob2)));

        jobZooKeeper.run();

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
        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        asList(succeededJob1, succeededJob2, aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build())
                );
        when(storageProvider.save(anyList())).thenThrow(new ConcurrentJobModificationException(asList(succeededJob1, succeededJob2)));

        for(int i = 0; i <= 5 ; i++) {
            jobZooKeeper.run();
        }

        verify(backgroundJobServer).stop();
    }

    private JobZooKeeper initializeJobZooKeeper() {
        UUID backgroundJobServerId = UUID.randomUUID();
        lenient().when(backgroundJobServer.getId()).thenReturn(backgroundJobServerId);
        lenient().when(backgroundJobServer.getServerStatus()).thenReturn(backgroundJobServerStatus);
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