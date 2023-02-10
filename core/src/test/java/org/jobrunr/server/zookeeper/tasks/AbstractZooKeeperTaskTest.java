package org.jobrunr.server.zookeeper.tasks;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobTestFilter;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.zookeeper.ZooKeeperStatistics;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
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

    BackgroundJobTestFilter logAllStateChangesFilter;
    ZooKeeperStatistics zooKeeperStatistics;
    ListAppender<ILoggingEvent> logger;

    @BeforeEach
    void setUpTaskDependencies() {
        logAllStateChangesFilter = new BackgroundJobTestFilter();
        lenient().when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        lenient().when(backgroundJobServer.getJobZooKeeper()).thenReturn(jobZooKeeper);
        lenient().when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters(logAllStateChangesFilter));
        lenient().when(backgroundJobServer.getConfiguration()).thenReturn(usingStandardBackgroundJobServerConfiguration());
        lenient().when(backgroundJobServer.getWorkDistributionStrategy()).thenReturn(new BasicWorkDistributionStrategy(backgroundJobServer, 2));
        zooKeeperStatistics = new ZooKeeperStatistics(null);
    }

    void runTask(ZooKeeperTask task) {
        runTask(task, aDefaultBackgroundJobServerStatus().build());
    }

    void runTask(ZooKeeperTask task, BackgroundJobServerStatus status) {
        logger = LoggerAssert.initFor(jobZooKeeper);
        task.run(zooKeeperStatistics.startRun(status));
    }

}
