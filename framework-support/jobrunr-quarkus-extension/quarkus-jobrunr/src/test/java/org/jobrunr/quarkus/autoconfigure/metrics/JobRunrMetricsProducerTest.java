package org.jobrunr.quarkus.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jobrunr.quarkus.autoconfigure.metrics.JobRunrMetricsProducer.BackgroundJobServerMetricsProducer;
import org.jobrunr.quarkus.autoconfigure.metrics.JobRunrMetricsProducer.StorageProviderMetricsProducer;
import org.jobrunr.quarkus.autoconfigure.server.JobRunrBackgroundJobServerProducer;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrInMemoryStorageProviderProducer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusComponentTest({JobRunrInMemoryStorageProviderProducer.class, JobRunrBackgroundJobServerProducer.class, JobRunrMetricsProducer.class, BackgroundJobServerMetricsProducer.class, StorageProviderMetricsProducer.class})
class JobRunrMetricsProducerTest {

    @Inject
    Instance<BackgroundJobServer> backgroundJobServerInstance;

    @Inject
    Instance<BackgroundJobServerMetricsBinder> backgroundJobServerMetricsBinderInstance;

    @Inject
    Instance<StorageProviderMetricsBinder> storageProviderMetricsBinderInstance;

    @InjectMock
    MeterRegistry meterRegistry;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.jobs.metrics.enabled", value = "true")
    void storageProviderMetricsBinderAreAutoConfiguredIfJobsMetricsAreEnabled() {
        assertThat(storageProviderMetricsBinderInstance.isResolvable()).isTrue();
        assertThat(storageProviderMetricsBinderInstance.get()).isInstanceOf(StorageProviderMetricsBinder.class);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.jobs.metrics.enabled", value = "false")
    void storageProviderMetricsBinderAreNotAutoConfiguredIfJobsMetricsAreDisabled() {
        assertThat(storageProviderMetricsBinderInstance.isResolvable()).isTrue();
        assertThat(storageProviderMetricsBinderInstance.get()).isNull();
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.metrics.enabled", value = "true")
    void backgroundJobServerMetricsBinderIsAutoConfiguredIgnoredIfEnabled() {
        Mockito.when(meterRegistry.more()).thenReturn(Mockito.mock(MeterRegistry.More.class));
        assertThat(backgroundJobServerMetricsBinderInstance.isResolvable()).isTrue();
        assertThat(backgroundJobServerMetricsBinderInstance.get()).isInstanceOf(BackgroundJobServerMetricsBinder.class);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.metrics.enabled", value = "false")
    void backgroundJobServerMetricsBinderIsNotAutoConfiguredIgnoredIfDisabled() {
        assertThat(backgroundJobServerMetricsBinderInstance.isResolvable()).isTrue();
        assertThat(backgroundJobServerMetricsBinderInstance.get()).isNull();
    }

    @Test
    @TestConfigProperty(key = "jobrunr.background-job-server.enabled", value = "false")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.metrics.enabled", value = "true")
    void backgroundJobServerMetricsBinderIsIgnoredIfBackgroundJobServerIsNotPresent() {
        assertThat(backgroundJobServerMetricsBinderInstance.isResolvable()).isTrue();
        assertThat(backgroundJobServerMetricsBinderInstance.get()).isNull();
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.metrics.enabled", value = "false")
    @TestConfigProperty(key = "quarkus.jobrunr.jobs.metrics.enabled", value = "false")
    void metricsBindersAreIgnoredIfTheyAreDisabled() {
        assertThat(backgroundJobServerInstance.isResolvable()).isTrue();
        assertThat(backgroundJobServerInstance.get()).isInstanceOf(BackgroundJobServer.class);

        assertThat(backgroundJobServerMetricsBinderInstance.isResolvable()).isTrue();
        assertThat(backgroundJobServerMetricsBinderInstance.get()).isNull();

        assertThat(storageProviderMetricsBinderInstance.isResolvable()).isTrue();
        assertThat(storageProviderMetricsBinderInstance.get()).isNull();
    }
}