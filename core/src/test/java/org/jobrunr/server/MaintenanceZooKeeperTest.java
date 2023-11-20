package org.jobrunr.server;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.SevereJobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.emptyJobList;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

@ExtendWith(MockitoExtension.class)
class MaintenanceZooKeeperTest {

    @Mock
    private StorageProvider storageProvider;
    @Mock
    private BackgroundJobServer backgroundJobServer;
    @Mock
    private WorkDistributionStrategy workDistributionStrategy;
    @Mock
    private JobZooKeeper jobZooKeeper;
    @Captor
    private ArgumentCaptor<JobRunrMetadata> jobRunrMetadataArgumentCaptor;

    private BackgroundJobServerStatus backgroundJobServerStatus;
    private MaintenanceZooKeeper maintenanceZooKeeper;
    private LogAllStateChangesFilter logAllStateChangesFilter;
    private ListAppender<ILoggingEvent> logger;

    @BeforeEach
    void setUpBackgroundMaintenanceZooKeeper() {
        BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();
        when(backgroundJobServer.getConfiguration()).thenReturn(backgroundJobServerConfiguration);
        logAllStateChangesFilter = new LogAllStateChangesFilter();
        backgroundJobServerStatus = aDefaultBackgroundJobServerStatus().withIsStarted().build();
        maintenanceZooKeeper = initializeMaintenanceZooKeeper();
        lenient().when(backgroundJobServer.getConcurrentJobModificationResolver())
                .thenReturn(
                        backgroundJobServerConfiguration
                                .getConcurrentJobModificationPolicy()
                                .toConcurrentJobModificationResolver(storageProvider, jobZooKeeper)
                );
        logger = LoggerAssert.initFor(maintenanceZooKeeper);
    }

    @Test
    void testUsesMaintenancePollIntervalInSeconds() {
        assertThat((Duration) getInternalState(
                getInternalState(maintenanceZooKeeper, "zooKeeperStatistics"), "pollIntervalDuration")
        ).isEqualTo(ofSeconds(60));
    }

    @Test
    void jobZooKeeperDoesNothingIfItIsNotInitialized() {
        when(backgroundJobServer.isUnAnnounced()).thenReturn(true);

        maintenanceZooKeeper.run();

        verifyNoInteractions(storageProvider);
    }

    @Test
    void severeJobRunrExceptionsAreLoggedToStorageProvider() {
        Job aSucceededJob1 = aSucceededJob().build();
        Job aSucceededJob2 = aSucceededJob().build();

        when(storageProvider.getJobById(aSucceededJob1.getId())).thenReturn(aSucceededJob1);
        when(storageProvider.getJobById(aSucceededJob2.getId())).thenReturn(aSucceededJob2);
        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), any(PageRequest.class)))
                .thenReturn(
                        asList(aSucceededJob1, aSucceededJob2, aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build()),
                        emptyJobList()
                );
        when(storageProvider.save(anyList())).thenThrow(new ConcurrentJobModificationException(asList(aSucceededJob1, aSucceededJob2)));

        maintenanceZooKeeper.run();

        verify(storageProvider).saveMetadata(jobRunrMetadataArgumentCaptor.capture());

        assertThat(jobRunrMetadataArgumentCaptor.getValue())
                .hasName(SevereJobRunrException.class.getSimpleName())
                .hasOwner("BackgroundJobServer " + backgroundJobServer.getId())
                .valueContains("## Runtime information");
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnSuccess() {
        Job job = aSucceededJob().build();

        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(), any(PageRequest.class)))
                .thenReturn(singletonList(job), emptyJobList());

        maintenanceZooKeeper.run();

        assertThat(logAllStateChangesFilter.getStateChanges(job)).containsExactly("SUCCEEDED->DELETED");
    }

    @Test
    void jobZooKeeperStopsIfTooManyExceptions() {
        Job aSucceededJob1 = aSucceededJob().build();
        Job aSucceededJob2 = aSucceededJob().build();

        when(storageProvider.getJobById(aSucceededJob1.getId())).thenReturn(aSucceededJob1);
        when(storageProvider.getJobById(aSucceededJob2.getId())).thenReturn(aSucceededJob2);
        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        asList(aSucceededJob1, aSucceededJob2, aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build())
                );
        when(storageProvider.save(anyList())).thenThrow(new ConcurrentJobModificationException(asList(aSucceededJob1, aSucceededJob2)));

        for (int i = 0; i <= 5; i++) {
            maintenanceZooKeeper.run();
        }

        verify(backgroundJobServer).stop();
        assertThat(logger).hasErrorMessage("FATAL - JobRunr encountered too many processing exceptions. Shutting down.");
    }

    private MaintenanceZooKeeper initializeMaintenanceZooKeeper() {
        UUID backgroundJobServerId = UUID.randomUUID();
        lenient().when(backgroundJobServer.getId()).thenReturn(backgroundJobServerId);
        lenient().when(backgroundJobServer.getServerStatus()).thenReturn(backgroundJobServerStatus);
        lenient().when(backgroundJobServer.isRunning()).thenReturn(true);
        lenient().when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        lenient().when(backgroundJobServer.getWorkDistributionStrategy()).thenReturn(workDistributionStrategy);
        lenient().when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters(logAllStateChangesFilter));
        lenient().when(backgroundJobServer.getDashboardNotificationManager()).thenReturn(new DashboardNotificationManager(backgroundJobServerId, storageProvider));
        lenient().when(workDistributionStrategy.canOnboardNewWork()).thenReturn(true);
        lenient().when(workDistributionStrategy.getWorkPageRequest()).thenReturn(ascOnUpdatedAt(10));
        lenient().when(backgroundJobServer.isAnnounced()).thenReturn(true);
        lenient().when(backgroundJobServer.isMaster()).thenReturn(true);
        return new MaintenanceZooKeeper(backgroundJobServer);
    }
}