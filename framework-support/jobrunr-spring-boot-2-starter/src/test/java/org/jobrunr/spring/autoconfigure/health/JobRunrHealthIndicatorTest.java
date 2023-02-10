package org.jobrunr.spring.autoconfigure.health;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JobRunrHealthIndicatorTest {

    @Mock
    private JobRunrProperties.BackgroundJobServer backgroundJobServerProperties;

    @Mock
    private ObjectProvider<BackgroundJobServer> backgroundJobServerProvider;

    @Mock
    private BackgroundJobServer backgroundJobServer;

    private JobRunrHealthIndicator jobRunrHealthIndicator;

    @BeforeEach
    void setUpHealthIndicator() {
        final JobRunrProperties jobRunrProperties = new JobRunrProperties();
        jobRunrProperties.setBackgroundJobServer(backgroundJobServerProperties);

        lenient().when(backgroundJobServerProvider.getObject()).thenReturn(backgroundJobServer);

        jobRunrHealthIndicator = new JobRunrHealthIndicator(jobRunrProperties, backgroundJobServerProvider);
    }

    @Test
    void givenDisabledBackgroundJobServer_ThenHealthIsUp() {
        when(backgroundJobServerProperties.isEnabled()).thenReturn(false);
        assertThat(jobRunrHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void givenEnabledBackgroundJobServerAndBackgroundJobServerRunning_ThenHealthIsUp() {
        when(backgroundJobServerProperties.isEnabled()).thenReturn(true);
        when(backgroundJobServer.isRunning()).thenReturn(true);

        assertThat(jobRunrHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void givenEnabledBackgroundJobServerAndBackgroundJobServerStopped_ThenHealthIsDown() {
        when(backgroundJobServerProperties.isEnabled()).thenReturn(true);
        when(backgroundJobServer.isRunning()).thenReturn(false);

        assertThat(jobRunrHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
    }
}