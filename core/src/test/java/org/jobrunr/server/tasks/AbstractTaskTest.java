package org.jobrunr.server.tasks;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.server.JobSteward;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.jobrunr.server.carbonaware.AbstractCarbonAwareWiremockTest;
import org.jobrunr.server.carbonaware.CarbonAwareJobManager;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.server.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractTaskTest extends AbstractCarbonAwareWiremockTest {

    private final JsonMapper jsonMapper = new JacksonJsonMapper();

    protected BackgroundJobServer backgroundJobServer;
    @Mock
    protected StorageProvider storageProvider;
    @Mock
    protected JobSteward jobSteward;
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
        BackgroundJobServerConfiguration configuration = usingStandardBackgroundJobServerConfiguration();
        setUpBackgroundJobServerConfiguration(configuration);
        backgroundJobServer = createBackgroundJobServerSpy(storageProvider, configuration);
        backgroundJobServer.setJobFilters(List.of(logAllStateChangesFilter));
        backgroundJobServer.start();

        zooKeeperStatistics = new TaskStatistics(null);
    }

    protected void setUpBackgroundJobServerConfiguration(BackgroundJobServerConfiguration configuration) {
        // hook to override settings
    }

    protected CarbonAwareJobManager setUpCarbonAwareJobManager() {
        return spy(new CarbonAwareJobManager(usingStandardCarbonAwareConfiguration().andCarbonIntensityApiUrl(carbonIntensityApiBaseUrl).andAreaCode("BE"), jsonMapper));
    }

    protected void runTask(Task task) {
        runTask(task, backgroundJobServer.getConfiguration());
    }

    protected void runTask(Task task, BackgroundJobServerConfigurationReader backgroundJobServerConfiguration) {
        logger = LoggerAssert.initFor(task);
        task.run(zooKeeperStatistics.startRun(backgroundJobServerConfiguration));
    }

    private BackgroundJobServer createBackgroundJobServerSpy(StorageProvider storageProvider, BackgroundJobServerConfiguration configuration) {
        return spy(new BackgroundJobServer(storageProvider, setUpCarbonAwareJobManager(), jsonMapper, null, configuration) {
            @Override
            protected JobSteward createJobSteward() {
                return jobSteward;
            }

            @Override
            public WorkDistributionStrategy getWorkDistributionStrategy() {
                return new BasicWorkDistributionStrategy(backgroundJobServer, 2);
            }
        });
    }

}
