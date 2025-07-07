package org.jobrunr.micronaut.autoconfigure.health;

import io.micronaut.health.HealthStatus;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration;
import org.jobrunr.server.BackgroundJobServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JobRunrHealthIndicatorTest {

    @Mock
    private JobRunrConfiguration jobRunrConfiguration;

    @Mock
    private JobRunrConfiguration.BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    @Mock
    private BackgroundJobServer backgroundJobServer;

    private TestableJobRunrHealthIndicator jobRunrHealthIndicator;

    @BeforeEach
    void setUpHealthIndicator() {
        when(jobRunrConfiguration.getBackgroundJobServer()).thenReturn(backgroundJobServerConfiguration);

        jobRunrHealthIndicator = new TestableJobRunrHealthIndicator(backgroundJobServer, jobRunrConfiguration);
    }

    @Test
    void givenDisabledBackgroundJobServer_ThenHealthIsUp() {
        when(backgroundJobServerConfiguration.isEnabled()).thenReturn(false);

        jobRunrHealthIndicator.getHealthInformation();

        assertThat(jobRunrHealthIndicator.getHealthStatus()).isEqualTo(HealthStatus.UP);
    }

    @Test
    void givenEnabledBackgroundJobServerAndBackgroundJobServerRunning_ThenHealthIsUp() {
        when(backgroundJobServerConfiguration.isEnabled()).thenReturn(true);
        when(backgroundJobServer.isRunning()).thenReturn(true);

        jobRunrHealthIndicator.getHealthInformation();

        assertThat(jobRunrHealthIndicator.getHealthStatus()).isEqualTo(HealthStatus.UP);
    }

    @Test
    void givenEnabledBackgroundJobServerAndBackgroundJobServerStopped_ThenHealthIsDown() {
        when(backgroundJobServerConfiguration.isEnabled()).thenReturn(true);
        when(backgroundJobServer.isRunning()).thenReturn(false);

        jobRunrHealthIndicator.getHealthInformation();

        assertThat(jobRunrHealthIndicator.getHealthStatus()).isEqualTo(HealthStatus.DOWN);
    }

    static class TestableJobRunrHealthIndicator extends JobRunrHealthIndicator {

        public TestableJobRunrHealthIndicator(BackgroundJobServer backgroundJobServer, JobRunrConfiguration configuration) {
            super(backgroundJobServer, configuration);
        }

        public HealthStatus getHealthStatus() {
            return healthStatus;
        }
    }
}
