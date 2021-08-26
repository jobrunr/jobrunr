package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import org.jobrunr.quarkus.autoconfigure.JobRunrConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrProducer;
import org.jobrunr.quarkus.autoconfigure.JobRunrStarter;
import org.jobrunr.quarkus.autoconfigure.health.JobRunrHealthCheck;
import org.jobrunr.quarkus.autoconfigure.metrics.JobRunrMetricsProducer;
import org.jobrunr.quarkus.autoconfigure.metrics.JobRunrMetricsStarter;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrElasticSearchStorageProviderProducer;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrInMemoryStorageProviderProducer;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrMongoDBStorageProviderProducer;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrSqlStorageProviderProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JobRunrExtensionProcessorTest {

    @Mock
    Capabilities capabilities;

    JobRunrConfiguration jobRunrConfiguration;

    JobRunrConfiguration.BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    JobRunrExtensionProcessor jobRunrExtensionProcessor;

    @BeforeEach
    void setUpExtensionProcessor() {
        jobRunrExtensionProcessor = new JobRunrExtensionProcessor();
        jobRunrConfiguration = new JobRunrConfiguration();
        backgroundJobServerConfiguration = new JobRunrConfiguration.BackgroundJobServerConfiguration();
        jobRunrConfiguration.backgroundJobServer = backgroundJobServerConfiguration;
        lenient().when(capabilities.isPresent(Capability.JSONB)).thenReturn(true);
    }

    @Test
    void producesJobRunrProducer() {
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrProducer.class.getName())
                .contains(JobRunrStarter.class.getName())
                .contains(JobRunrInMemoryStorageProviderProducer.class.getName())
                .contains(JobRunrProducer.JobRunrJsonBJsonMapperProducer.class.getName());
    }

    @Test
    void producesJobRunrProducerUsesJSONBIfCapabilityPresent() {
        Mockito.reset(capabilities);
        lenient().when(capabilities.isPresent(Capability.JSONB)).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrProducer.JobRunrJsonBJsonMapperProducer.class.getName());
    }

    @Test
    void producesJobRunrProducerUsesJacksonIfCapabilityPresent() {
        Mockito.reset(capabilities);
        lenient().when(capabilities.isPresent(Capability.JACKSON)).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrProducer.JobRunrJacksonJsonMapperProducer.class.getName());
    }

    @Test
    void producesJobRunrProducerUsesSqlStorageProviderIfAgroalCapabilityIsPresent() {
        lenient().when(capabilities.isPresent(Capability.AGROAL)).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrSqlStorageProviderProducer.class.getName());
    }

    @Test
    void producesJobRunrProducerUsesMongoDBStorageProviderIfMongoDBClientCapabilityIsPresent() {
        lenient().when(capabilities.isPresent(Capability.MONGODB_CLIENT)).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrMongoDBStorageProviderProducer.class.getName());
    }

    @Test
    void producesJobRunrProducerUsesElasticSearchStorageProviderIfElasticSearchRestHighLevelClientCapabilityIsPresent() {
        lenient().when(capabilities.isPresent(Capability.ELASTICSEARCH_REST_HIGH_LEVEL_CLIENT)).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrElasticSearchStorageProviderProducer.class.getName());
    }

    @Test
    void addHealthCheckAddsHealthBuildItemIfSmallRyeHealthCapabilityIsPresent() {
        lenient().when(capabilities.isPresent(Capability.SMALLRYE_HEALTH)).thenReturn(true);
        final HealthBuildItem healthBuildItem = jobRunrExtensionProcessor.addHealthCheck(capabilities, jobRunrConfiguration);

        assertThat(healthBuildItem.getHealthCheckClass())
                .isEqualTo(JobRunrHealthCheck.class.getName());
    }

    @Test
    void addHealthCheckDoesNotAddHealthBuildItemIfSmallRyeHealthCapabilityIsNotPresent() {
        lenient().when(capabilities.isPresent(Capability.SMALLRYE_HEALTH)).thenReturn(false);
        final HealthBuildItem healthBuildItem = jobRunrExtensionProcessor.addHealthCheck(capabilities, jobRunrConfiguration);

        assertThat(healthBuildItem).isNull();
    }

    @Test
    void addMetricsDoesNotAddMetricsIfNotEnabled() {
        final AdditionalBeanBuildItem metricsBeanBuildItem = jobRunrExtensionProcessor.addMetrics(Optional.empty(), jobRunrConfiguration);

        assertThat(metricsBeanBuildItem).isNull();
    }

    @Test
    void addMetricsDoesNotAddMetricsIfEnabledButNoMicroMeterSupport() {
        final AdditionalBeanBuildItem metricsBeanBuildItem = jobRunrExtensionProcessor.addMetrics(Optional.of(new MetricsCapabilityBuildItem(toSupport -> false)), jobRunrConfiguration);

        assertThat(metricsBeanBuildItem).isNull();
    }

    @Test
    void addMetricsDoesAddStorageProviderMetricsIfEnabledAndMicroMeterSupport() {
        final AdditionalBeanBuildItem metricsBeanBuildItem = jobRunrExtensionProcessor.addMetrics(Optional.of(new MetricsCapabilityBuildItem(toSupport -> toSupport.equals(MetricsFactory.MICROMETER))), jobRunrConfiguration);

        assertThat(metricsBeanBuildItem.getBeanClasses())
                .contains(JobRunrMetricsStarter.class.getName())
                .contains(JobRunrMetricsProducer.StorageProviderMetricsProducer.class.getName())
                .doesNotContain(JobRunrMetricsProducer.BackgroundJobServerMetricsProducer.class.getName());
    }

    @Test
    void addMetricsDoesAddStorageProviderAndBackgroundJobServerMetricsIfEnabledAndMicroMeterSupport() {
        backgroundJobServerConfiguration.enabled = true;

        final AdditionalBeanBuildItem metricsBeanBuildItem = jobRunrExtensionProcessor.addMetrics(Optional.of(new MetricsCapabilityBuildItem(toSupport -> toSupport.equals(MetricsFactory.MICROMETER))), jobRunrConfiguration);

        assertThat(metricsBeanBuildItem.getBeanClasses())
                .contains(JobRunrMetricsStarter.class.getName())
                .contains(JobRunrMetricsProducer.StorageProviderMetricsProducer.class.getName())
                .contains(JobRunrMetricsProducer.BackgroundJobServerMetricsProducer.class.getName());
    }

}