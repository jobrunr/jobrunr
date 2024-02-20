package org.jobrunr.quarkus.autoconfigure.health;

import jakarta.enterprise.inject.Instance;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jobrunr.quarkus.autoconfigure.JobRunrBuildTimeConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrBuildTimeConfiguration.BackgroundJobServerConfiguration;
import org.jobrunr.server.BackgroundJobServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRunrHealthCheckTest {

    @Mock
    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;

    @Mock
    BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    @Mock
    private Instance<BackgroundJobServer> backgroundJobServerProviderInstance;

    @Mock
    private BackgroundJobServer backgroundJobServer;

    private JobRunrHealthCheck jobRunrHealthCheck;

    @BeforeEach
    void setUpHealthIndicator() {
        when(jobRunrBuildTimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerConfiguration);

        lenient().when(backgroundJobServerProviderInstance.get()).thenReturn(backgroundJobServer);

        jobRunrHealthCheck = new JobRunrHealthCheck(jobRunrBuildTimeConfiguration, backgroundJobServerProviderInstance);
    }

    @Test
    void givenDisabledBackgroundJobServer_ThenHealthIsOutOfService() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(false);
        assertThat(jobRunrHealthCheck.call().getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    void givenEnabledBackgroundJobServerAndBackgroundJobServerRunning_ThenHealthIsUp() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(true);
        when(backgroundJobServer.isRunning()).thenReturn(true);

        assertThat(jobRunrHealthCheck.call().getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    void givenEnabledBackgroundJobServerAndBackgroundJobServerStopped_ThenHealthIsDown() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(true);
        when(backgroundJobServer.isRunning()).thenReturn(false);

        assertThat(jobRunrHealthCheck.call().getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
    }
}