package org.jobrunr.micronaut.autoconfigure.metrics;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.server.metrics.BackgroundJobServerMetricsBinder;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.jobrunr.JobRunrAssertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JobRunrMetricsStarterTest {

    @Mock
    StorageProviderMetricsBinder storageProviderMetricsBinder;

    @Mock
    BackgroundJobServerMetricsBinder backgroundJobServerMetricsBinder;

    @Test
    void onStartOptionalsAreCalledToBootstrapBinders() {
        final JobRunrMetricsStarter jobRunrMetricsStarter = new JobRunrMetricsStarter(Optional.of(storageProviderMetricsBinder), Optional.of(backgroundJobServerMetricsBinder));
        ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(jobRunrMetricsStarter);

        jobRunrMetricsStarter.startup(null);

        assertThat(logger)
                .hasDebugMessageContaining("JobRunr StorageProvider MicroMeter Metrics enabled")
                .hasDebugMessageContaining("JobRunr BackgroundJobServer MicroMeter Metrics enabled");
    }

}