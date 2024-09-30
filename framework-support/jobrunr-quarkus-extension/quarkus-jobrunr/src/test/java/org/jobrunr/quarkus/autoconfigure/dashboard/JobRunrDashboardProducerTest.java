package org.jobrunr.quarkus.autoconfigure.dashboard;

import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration.DashboardConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration.MiscellaneousConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@ExtendWith(MockitoExtension.class)
class JobRunrDashboardProducerTest {
    JobRunrDashboardProducer jobRunrDashboardProducer;

    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;
    @Mock
    DashboardConfiguration dashboardRunTimeConfiguration;
    @Mock
    MiscellaneousConfiguration miscellaneousRunTimeConfiguration;

    @Mock
    StorageProvider storageProvider;

    @Mock
    JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        lenient().when(jobRunrRuntimeConfiguration.dashboard()).thenReturn(dashboardRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.miscellaneous()).thenReturn(miscellaneousRunTimeConfiguration);

        jobRunrDashboardProducer = new JobRunrDashboardProducer();
        setInternalState(jobRunrDashboardProducer, "jobRunrRuntimeConfiguration", jobRunrRuntimeConfiguration);
    }

    @Test
    void dashboardWebServerConfigurationIsNotSetupWhenNotConfigured() {
        when(dashboardRunTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrDashboardProducer.dashboardWebServerConfiguration()).isNull();
    }

    @Test
    void dashboardWebServerConfigurationIsSetupWhenConfigured() {
        when(dashboardRunTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrDashboardProducer.dashboardWebServerConfiguration()).isNotNull();
    }

    @Test
    void dashboardWebServerIsNotSetupWhenNotConfigured() {
        when(dashboardRunTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrDashboardProducer.dashboardWebServer(storageProvider, jsonMapper, usingStandardDashboardConfiguration())).isNull();
    }

    @Test
    void dashboardWebServerIsSetupWhenConfigured() {
        when(dashboardRunTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrDashboardProducer.dashboardWebServer(storageProvider, jsonMapper, usingStandardDashboardConfiguration())).isNotNull();
    }
}