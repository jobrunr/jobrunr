package org.jobrunr.quarkus.autoconfigure.server;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.server.threadpool.PlatformThreadPoolJobRunrExecutor;
import org.junit.jupiter.api.Test;

import static org.jobrunr.JobRunrAssertions.assertThat;

@QuarkusComponentTest
public class JobRunrBackgroundJobServerProducerCustomWorkerPolicyTest {
    // Injection needed to create all other beans otherwise the extension doesn't pick them up.
    @Inject
    JobRunrBackgroundJobServerProducer jobRunrBackgroundJobServerProducer;

    @Inject
    BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    void backgroundJobServerAutoConfigurationTakesIntoAccountCustomBackgroundJobServerWorkerPolicy() {
        assertThat(backgroundJobServerConfiguration)
                .hasWorkerPolicyOfType(BackgroundJobServerConfigurationWithCustomWorkerPolicy.MyBackgroundJobServerWorkerPolicy.class);
    }

    @Singleton
    static class BackgroundJobServerConfigurationWithCustomWorkerPolicy {

        @Produces
        public BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy() {
            return new MyBackgroundJobServerWorkerPolicy();
        }

        private static class MyBackgroundJobServerWorkerPolicy implements BackgroundJobServerWorkerPolicy {

            @Override
            public WorkDistributionStrategy toWorkDistributionStrategy(BackgroundJobServer backgroundJobServer) {
                return new BasicWorkDistributionStrategy(backgroundJobServer, 10);
            }

            @Override
            public JobRunrExecutor toJobRunrExecutor() {
                return new PlatformThreadPoolJobRunrExecutor(10, "my-prefix");
            }
        }
    }
}
