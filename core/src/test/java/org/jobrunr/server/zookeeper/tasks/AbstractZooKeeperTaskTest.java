package org.jobrunr.server.zookeeper.tasks;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.zookeeper.ZooKeeperStatistics;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractZooKeeperTaskTest {

    @Mock
    JobZooKeeper jobZooKeeper;
    @Mock
    BackgroundJobServer backgroundJobServer;
    @Mock
    StorageProvider storageProvider;
    @Captor
    ArgumentCaptor<List<Job>> jobsToSaveArgumentCaptor;

    LogAllStateChangesFilter logAllStateChangesFilter;
    ZooKeeperStatistics zooKeeperStatistics;
    ListAppender<ILoggingEvent> logger;
    private BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    @BeforeEach
    void setUpTaskDependencies() {
        backgroundJobServerConfiguration = getBackgroundJobServerConfiguration();

        logAllStateChangesFilter = new LogAllStateChangesFilter();
        lenient().when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        lenient().when(backgroundJobServer.getJobZooKeeper()).thenReturn(jobZooKeeper);
        lenient().when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters(logAllStateChangesFilter));
        lenient().when(backgroundJobServer.getConfiguration()).thenReturn(backgroundJobServerConfiguration);
        lenient().when(backgroundJobServer.getWorkDistributionStrategy()).thenReturn(new BasicWorkDistributionStrategy(backgroundJobServer, 2));
        lenient()
                .when(backgroundJobServer.getConcurrentJobModificationResolver())
                .thenReturn(
                        backgroundJobServerConfiguration
                                .getConcurrentJobModificationPolicy()
                                .toConcurrentJobModificationResolver(storageProvider, jobZooKeeper)
                );
        zooKeeperStatistics = new ZooKeeperStatistics("MaintenanceZooKeeper", 15, null);
    }

    void runTask(ZooKeeperTask task) {
        logger = LoggerAssert.initFor(jobZooKeeper);
        task.run(zooKeeperStatistics.startRun());
    }

    protected BackgroundJobServerConfiguration getBackgroundJobServerConfiguration() {
        return usingStandardBackgroundJobServerConfiguration();
    }

}
