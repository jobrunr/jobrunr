package org.jobrunr.server.zookeeper.tasks;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.zookeeper.ZooKeeperStatistics;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.Mocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractZooKeeperTaskTest {

    BackgroundJobServer backgroundJobServer = Mocks.ofBackgroundJobServer();
    @Mock
    JobZooKeeper jobZooKeeper;
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
        setUpTaskDependencies(storageProvider);
    }

    void setUpTaskDependencies(StorageProvider storageProvider) {
        logAllStateChangesFilter = new LogAllStateChangesFilter();
        lenient().when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        lenient().when(backgroundJobServer.getJobZooKeeper()).thenReturn(jobZooKeeper);
        lenient().when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters(logAllStateChangesFilter));
        lenient().when(backgroundJobServer.getWorkDistributionStrategy()).thenReturn(new BasicWorkDistributionStrategy(backgroundJobServer, 2));
        zooKeeperStatistics = new ZooKeeperStatistics(null);
    }

    void runTask(ZooKeeperTask task) {
        runTask(task, backgroundJobServer.getConfiguration());
    }

    void runTask(ZooKeeperTask task, BackgroundJobServerConfigurationReader backgroundJobServerConfiguration) {
        logger = LoggerAssert.initFor(jobZooKeeper);
        task.run(zooKeeperStatistics.startRun(backgroundJobServerConfiguration));
    }

}
