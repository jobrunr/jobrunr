package org.jobrunr.server.tasks;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.server.JobSteward;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
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

    protected BackgroundJobServer backgroundJobServer = Mocks.ofBackgroundJobServer();
    @Mock
    protected JobSteward jobSteward;
    @Mock
    protected StorageProvider storageProvider;
    @Captor
    protected ArgumentCaptor<List<Job>> jobsToSaveArgumentCaptor;

    protected LogAllStateChangesFilter logAllStateChangesFilter;
    protected TaskStatistics zooKeeperStatistics;
    protected ListAppender<ILoggingEvent> logger;

    @BeforeEach
    protected void setUpTaskDependencies() {
        setUpTaskDependencies(storageProvider);
    }

    protected void setUpTaskDependencies(StorageProvider storageProvider) {
        logAllStateChangesFilter = new LogAllStateChangesFilter();
        lenient().when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        lenient().when(backgroundJobServer.getJobSteward()).thenReturn(jobSteward);
        lenient().when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters(logAllStateChangesFilter));
        lenient().when(backgroundJobServer.getWorkDistributionStrategy()).thenReturn(new BasicWorkDistributionStrategy(backgroundJobServer, 2));
        zooKeeperStatistics = new TaskStatistics(null);
    }

    protected void runTask(Task task) {
        runTask(task, backgroundJobServer.getConfiguration());
    }

    protected void runTask(Task task, BackgroundJobServerConfigurationReader backgroundJobServerConfiguration) {
        logger = LoggerAssert.initFor(jobSteward);
        task.run(zooKeeperStatistics.startRun(backgroundJobServerConfiguration));
    }

}
