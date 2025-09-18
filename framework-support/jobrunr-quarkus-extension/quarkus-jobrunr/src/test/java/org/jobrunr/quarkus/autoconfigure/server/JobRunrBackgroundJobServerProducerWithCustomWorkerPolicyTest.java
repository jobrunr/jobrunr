package org.jobrunr.quarkus.autoconfigure.server;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.assertj.core.api.Assertions;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.server.threadpool.PlatformThreadPoolJobRunrExecutor;
import org.junit.jupiter.api.Test;

import static org.jobrunr.JobRunrAssertions.assertThat;

@QuarkusComponentTest(JobRunrBackgroundJobServerProducer.class)
public class JobRunrBackgroundJobServerProducerWithCustomWorkerPolicyTest {

    @Inject
    Instance<BackgroundJobServerConfiguration> backgroundJobServerConfigurationInstance;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    void backgroundJobServerAutoConfigurationTakesIntoAccountCustomBackgroundJobServerWorkerPolicy() {
        assertThat(backgroundJobServerConfigurationInstance.isResolvable()).isTrue();
        Assertions.assertThat(backgroundJobServerConfigurationInstance.get()).isInstanceOf(BackgroundJobServerConfiguration.class);

        assertThat(backgroundJobServerConfigurationInstance.get())
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
