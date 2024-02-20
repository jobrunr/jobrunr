package org.jobrunr.quarkus.autoconfigure.metrics;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRunrMetricsStarterTest {

    @Mock
    Instance<StorageProviderMetricsBinder> storageProviderMetricsBinderInstance;

    @Mock
    Instance<BackgroundJobServerMetricsBinder> backgroundJobServerMetricsBinderInstance;

    JobRunrMetricsStarter jobRunrMetricsStarter;

    @BeforeEach
    void setUpJobRunrMetricsStarter() {
        jobRunrMetricsStarter = new JobRunrMetricsStarter(storageProviderMetricsBinderInstance, backgroundJobServerMetricsBinderInstance);
    }

    @Test
    void metricsStarterDoesNotStartStorageProviderMetricsBinderIfNotAvailable() {
        jobRunrMetricsStarter.startup(new StartupEvent());

        verify(storageProviderMetricsBinderInstance, never()).get();
    }

    @Test
    void metricsStarterStartsStorageProviderMetricsBinderIfAvailable() {
        when(storageProviderMetricsBinderInstance.isResolvable()).thenReturn(true);

        jobRunrMetricsStarter.startup(new StartupEvent());

        verify(storageProviderMetricsBinderInstance).get();
    }

    @Test
    void metricsStarterDoesNotStartBackgroundJobServerMetricsBinderIfNotAvailable() {
        jobRunrMetricsStarter.startup(new StartupEvent());

        verify(backgroundJobServerMetricsBinderInstance, never()).get();
    }

    @Test
    void metricsStarterStartsBackgroundJobServerMetricsBinderIfAvailable() {
        when(backgroundJobServerMetricsBinderInstance.isResolvable()).thenReturn(true);

        jobRunrMetricsStarter.startup(new StartupEvent());

        verify(backgroundJobServerMetricsBinderInstance).get();
    }

}