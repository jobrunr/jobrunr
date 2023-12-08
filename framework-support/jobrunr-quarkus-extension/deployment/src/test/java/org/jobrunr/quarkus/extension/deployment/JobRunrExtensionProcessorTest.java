package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import org.jboss.jandex.IndexView;
import org.jobrunr.quarkus.autoconfigure.JobRunrBuildTimeConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrBuildTimeConfiguration.BackgroundJobServerConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrBuildTimeConfiguration.DatabaseConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrBuildTimeConfiguration.JobSchedulerConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrProducer;
import org.jobrunr.quarkus.autoconfigure.JobRunrStarter;
import org.jobrunr.quarkus.autoconfigure.health.JobRunrHealthCheck;
import org.jobrunr.quarkus.autoconfigure.metrics.JobRunrMetricsProducer;
import org.jobrunr.quarkus.autoconfigure.metrics.JobRunrMetricsStarter;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrDocumentDBStorageProviderProducer;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrInMemoryStorageProviderProducer;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrMongoDBStorageProviderProducer;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrSqlStorageProviderProducer;
import org.jobrunr.scheduling.JobRunrRecurringJobRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobRunrExtensionProcessorTest {

    @Mock
    Capabilities capabilities;

    @Mock
    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;

    @Mock
    JobSchedulerConfiguration jobSchedulerConfiguration;

    @Mock
    BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    @Mock
    DatabaseConfiguration databaseConfiguration;

    JobRunrExtensionProcessor jobRunrExtensionProcessor;

    @BeforeEach
    void setUpExtensionProcessor() {
        jobRunrExtensionProcessor = new JobRunrExtensionProcessor();
        lenient().when(jobRunrBuildTimeConfiguration.database()).thenReturn(databaseConfiguration);
        lenient().when(jobRunrBuildTimeConfiguration.jobScheduler()).thenReturn(jobSchedulerConfiguration);
        lenient().when(jobRunrBuildTimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerConfiguration);
        lenient().when(capabilities.isPresent(Capability.JSONB)).thenReturn(true);
    }

    @Test
    void producesJobRunrProducer() {
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .containsOnly(
                        JobRunrProducer.class.getName(),
                        JobRunrStarter.class.getName(),
                        JobRunrInMemoryStorageProviderProducer.class.getName(),
                        JobRunrProducer.JobRunrJsonBJsonMapperProducer.class.getName()
                );
    }

    @Test
    void producesJobRunrRecurringJobsFinderIfJobSchedulerIsEnabled() throws NoSuchMethodException {
        RecorderContext recorderContext = mock(RecorderContext.class);
        CombinedIndexBuildItem combinedIndex = mock(CombinedIndexBuildItem.class);
        when(combinedIndex.getIndex()).thenReturn(mock(IndexView.class));
        BeanContainerBuildItem beanContainer = mock(BeanContainerBuildItem.class);
        JobRunrRecurringJobRecorder recurringJobRecorder = mock(JobRunrRecurringJobRecorder.class);

        when(jobSchedulerConfiguration.enabled()).thenReturn(true);

        jobRunrExtensionProcessor.findRecurringJobAnnotationsAndScheduleThem(recorderContext, combinedIndex, beanContainer, recurringJobRecorder, jobRunrBuildTimeConfiguration);

        verify(recorderContext, times(2)).registerNonDefaultConstructor(any(), any());
    }

    @Test
    void producesNoJobRunrRecurringJobsFinderIfJobSchedulerIsNotEnabled() throws NoSuchMethodException {
        RecorderContext recorderContext = mock(RecorderContext.class);
        CombinedIndexBuildItem combinedIndex = mock(CombinedIndexBuildItem.class);
        BeanContainerBuildItem beanContainer = mock(BeanContainerBuildItem.class);
        JobRunrRecurringJobRecorder recurringJobRecorder = mock(JobRunrRecurringJobRecorder.class);

        when(jobSchedulerConfiguration.enabled()).thenReturn(false);

        jobRunrExtensionProcessor.findRecurringJobAnnotationsAndScheduleThem(recorderContext, combinedIndex, beanContainer, recurringJobRecorder, jobRunrBuildTimeConfiguration);

        verifyNoInteractions(recorderContext);
    }

    @Test
    void jobRunrProducerUsesJSONBIfCapabilityPresent() {
        Mockito.reset(capabilities);
        lenient().when(capabilities.isPresent(Capability.JSONB)).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrProducer.JobRunrJsonBJsonMapperProducer.class.getName());
    }

    @Test
    void jobRunrProducerUsesJacksonIfCapabilityPresent() {
        Mockito.reset(capabilities);
        lenient().when(capabilities.isPresent(Capability.JACKSON)).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrProducer.JobRunrJacksonJsonMapperProducer.class.getName());
    }

    @Test
    void jobRunrProducerUsesSqlStorageProviderIfAgroalCapabilityIsPresent() {
        lenient().when(capabilities.isPresent(Capability.AGROAL)).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrSqlStorageProviderProducer.class.getName());
    }

    @Test
    void jobRunrProducerUsesMongoDBStorageProviderIfMongoDBClientCapabilityIsPresent() {
        lenient().when(capabilities.isPresent(Capability.MONGODB_CLIENT)).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrMongoDBStorageProviderProducer.class.getName())
                .doesNotContain(JobRunrDocumentDBStorageProviderProducer.class.getName());
    }

    @Test
    void jobRunrProducerUsesDocumentDBStorageProviderIfMongoDBClientCapabilityIsPresent() {
        lenient().when(capabilities.isPresent(Capability.MONGODB_CLIENT)).thenReturn(true);
        when(databaseConfiguration.type()).thenReturn(Optional.of("documentdb"));
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrDocumentDBStorageProviderProducer.class.getName())
                .doesNotContain(JobRunrMongoDBStorageProviderProducer.class.getName());
    }

    @Test
    void addHealthCheckAddsHealthBuildItemIfSmallRyeHealthCapabilityIsPresent() {
        lenient().when(capabilities.isPresent(Capability.SMALLRYE_HEALTH)).thenReturn(true);
        final HealthBuildItem healthBuildItem = jobRunrExtensionProcessor.addHealthCheck(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(healthBuildItem.getHealthCheckClass())
                .isEqualTo(JobRunrHealthCheck.class.getName());
    }

    @Test
    void addHealthCheckDoesNotAddHealthBuildItemIfSmallRyeHealthCapabilityIsNotPresent() {
        lenient().when(capabilities.isPresent(Capability.SMALLRYE_HEALTH)).thenReturn(false);
        final HealthBuildItem healthBuildItem = jobRunrExtensionProcessor.addHealthCheck(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(healthBuildItem).isNull();
    }

    @Test
    void addMetricsDoesNotAddMetricsIfNotEnabled() {
        final AdditionalBeanBuildItem metricsBeanBuildItem = jobRunrExtensionProcessor.addMetrics(Optional.empty(), jobRunrBuildTimeConfiguration);

        assertThat(metricsBeanBuildItem).isNull();
    }

    @Test
    void addMetricsDoesNotAddMetricsIfEnabledButNoMicroMeterSupport() {
        final AdditionalBeanBuildItem metricsBeanBuildItem = jobRunrExtensionProcessor.addMetrics(Optional.of(new MetricsCapabilityBuildItem(toSupport -> false)), jobRunrBuildTimeConfiguration);

        assertThat(metricsBeanBuildItem).isNull();
    }

    @Test
    void addMetricsDoesAddStorageProviderMetricsIfEnabledAndMicroMeterSupport() {
        final AdditionalBeanBuildItem metricsBeanBuildItem = jobRunrExtensionProcessor.addMetrics(Optional.of(new MetricsCapabilityBuildItem(toSupport -> toSupport.equals(MetricsFactory.MICROMETER))), jobRunrBuildTimeConfiguration);

        assertThat(metricsBeanBuildItem.getBeanClasses())
                .contains(JobRunrMetricsStarter.class.getName())
                .contains(JobRunrMetricsProducer.StorageProviderMetricsProducer.class.getName())
                .doesNotContain(JobRunrMetricsProducer.BackgroundJobServerMetricsProducer.class.getName());
    }

    @Test
    void addMetricsDoesAddStorageProviderAndBackgroundJobServerMetricsIfEnabledAndMicroMeterSupport() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(true);

        final AdditionalBeanBuildItem metricsBeanBuildItem = jobRunrExtensionProcessor.addMetrics(Optional.of(new MetricsCapabilityBuildItem(toSupport -> toSupport.equals(MetricsFactory.MICROMETER))), jobRunrBuildTimeConfiguration);

        assertThat(metricsBeanBuildItem.getBeanClasses())
                .contains(JobRunrMetricsStarter.class.getName())
                .contains(JobRunrMetricsProducer.StorageProviderMetricsProducer.class.getName())
                .contains(JobRunrMetricsProducer.BackgroundJobServerMetricsProducer.class.getName());
    }

}