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
import org.jobrunr.quarkus.autoconfigure.JobRunrProducer;
import org.jobrunr.quarkus.autoconfigure.JobRunrStarter;
import org.jobrunr.quarkus.autoconfigure.dashboard.JobRunrDashboardProducer;
import org.jobrunr.quarkus.autoconfigure.health.JobRunrHealthCheck;
import org.jobrunr.quarkus.autoconfigure.metrics.JobRunrMetricsProducer;
import org.jobrunr.quarkus.autoconfigure.metrics.JobRunrMetricsStarter;
import org.jobrunr.quarkus.autoconfigure.server.JobRunrBackgroundJobServerProducer;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRunrExtensionProcessorTest {

    @Mock
    Capabilities capabilities;

    @Mock
    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;
    @Mock
    JobRunrBuildTimeConfiguration.BackgroundJobServerConfiguration backgroundJobServerConfiguration;
    @Mock
    JobRunrBuildTimeConfiguration.DashboardConfiguration dashboardConfiguration;
    @Mock
    JobRunrBuildTimeConfiguration.DatabaseConfiguration databaseConfiguration;

    JobRunrExtensionProcessorForTesting jobRunrExtensionProcessor;

    @BeforeEach
    void setUpExtensionProcessor() {
        jobRunrExtensionProcessor = new JobRunrExtensionProcessorForTesting();
        lenient().when(jobRunrBuildTimeConfiguration.database()).thenReturn(databaseConfiguration);
        lenient().when(jobRunrBuildTimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerConfiguration);
        lenient().when(jobRunrBuildTimeConfiguration.dashboard()).thenReturn(dashboardConfiguration);

        lenient().when(capabilities.isPresent(Capability.JSONB)).thenReturn(true);
    }

    @Test
    void producesJobRunrProducer() {
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .containsOnly(
                        JobRunrProducer.class.getName(),
                        JobRunrStarter.class.getName(),
                        JobRunrProducer.JobRunrJsonBJsonMapperProducer.class.getName()
                );
    }

    @Test
    void producesBackgroundJobServerWhenIncluded() {
        when(backgroundJobServerConfiguration.included()).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.addBackgroundJobServer(jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrBackgroundJobServerProducer.class.getName());
    }

    @Test
    void producesNoBackgroundJobServerWhenNotIncluded() {
        when(backgroundJobServerConfiguration.included()).thenReturn(false);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.addBackgroundJobServer(jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem).isNull();
    }

    @Test
    void producesDashboardWhenIncluded() {
        when(dashboardConfiguration.included()).thenReturn(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.addDashboard(jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrDashboardProducer.class.getName());
    }

    @Test
    void producesNoDashboardWhenNotIncluded() {
        when(dashboardConfiguration.included()).thenReturn(false);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.addDashboard(jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem).isNull();
    }

    @Test
    void producesJobRunrRecurringJobsFinder() throws NoSuchMethodException {
        RecorderContext recorderContext = mock(RecorderContext.class);
        CombinedIndexBuildItem combinedIndex = mock(CombinedIndexBuildItem.class);
        when(combinedIndex.getIndex()).thenReturn(mock(IndexView.class));
        BeanContainerBuildItem beanContainer = mock(BeanContainerBuildItem.class);
        JobRunrRecurringJobRecorder recurringJobRecorder = mock(JobRunrRecurringJobRecorder.class);

        jobRunrExtensionProcessor.findRecurringJobAnnotationsAndScheduleThem(recorderContext, combinedIndex, beanContainer, recurringJobRecorder);

        verify(recorderContext, times(2)).registerNonDefaultConstructor(any(), any());
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
    void jobRunrProducerUsesKotlinxSerializationIfPresent() {
        jobRunrExtensionProcessor.setKotlinxSerializationWillBePresent(true);
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrProducer.JobRunrKotlinxSerializataionJsonMapperProducer.class.getName());
    }

    @Test
    void kotlinxSerializationClassRetrievalIsInSyncWithKotlinLanguageSupportProject() throws IOException {
        var kotlinSrc = new String(Files.readAllBytes(Paths.get("../../../language-support/jobrunr-kotlin-22-support/src/main/kotlin/org/jobrunr/kotlin/utils/mapper/KotlinxSerializationJsonMapper.kt")), UTF_8);
        var processorSrc = new String(Files.readAllBytes(Paths.get("src/main/java/org/jobrunr/quarkus/extension/deployment/JobRunrExtensionProcessor.java")), UTF_8);

        assertThat(kotlinSrc).contains("class KotlinxSerializationJsonMapper");
        assertThat(processorSrc).contains("classExists(\"org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper\")");
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
    void jobRunrProducerThrowsIllegalStateWhenNoJsonCapabilityPresent() {
        Mockito.reset(capabilities);
        jobRunrExtensionProcessor.setKotlinxSerializationWillBePresent(false);
        lenient().when(capabilities.isPresent(Capability.JACKSON)).thenReturn(false);
        lenient().when(capabilities.isPresent(Capability.JSONB)).thenReturn(false);

        assertThatCode(() -> jobRunrExtensionProcessor.produce(capabilities, jobRunrBuildTimeConfiguration)).isInstanceOf(IllegalStateException.class);
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
    void jobRunrProducerUsesInMemoryStorageProviderIfDatabaseTypeIsEqualToMem() {
        when(databaseConfiguration.type()).thenReturn(Optional.of("mem"));
        final AdditionalBeanBuildItem additionalBeanBuildItem = jobRunrExtensionProcessor.produce(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(additionalBeanBuildItem.getBeanClasses())
                .contains(JobRunrInMemoryStorageProviderProducer.class.getName());
    }

    @Test
    void addHealthCheckAddsHealthBuildItemIfSmallRyeHealthCapabilityIsPresentAndBackgroundJobServerIsIncluded() {
        when(capabilities.isPresent(Capability.SMALLRYE_HEALTH)).thenReturn(true);
        when(backgroundJobServerConfiguration.included()).thenReturn(true);
        final HealthBuildItem healthBuildItem = jobRunrExtensionProcessor.addHealthCheck(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(healthBuildItem.getHealthCheckClass())
                .isEqualTo(JobRunrHealthCheck.class.getName());
    }

    @Test
    void addHealthCheckDoesNotAddHealthBuildItemIfSmallRyeHealthCapabilityIsPresentButBackgroundJobServerIsNotIncluded() {
        when(capabilities.isPresent(Capability.SMALLRYE_HEALTH)).thenReturn(true);
        when(backgroundJobServerConfiguration.included()).thenReturn(false);
        final HealthBuildItem healthBuildItem = jobRunrExtensionProcessor.addHealthCheck(capabilities, jobRunrBuildTimeConfiguration);

        assertThat(healthBuildItem).isNull();
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
        when(backgroundJobServerConfiguration.included()).thenReturn(true);

        final AdditionalBeanBuildItem metricsBeanBuildItem = jobRunrExtensionProcessor.addMetrics(Optional.of(new MetricsCapabilityBuildItem(toSupport -> toSupport.equals(MetricsFactory.MICROMETER))), jobRunrBuildTimeConfiguration);

        assertThat(metricsBeanBuildItem.getBeanClasses())
                .contains(JobRunrMetricsStarter.class.getName())
                .contains(JobRunrMetricsProducer.StorageProviderMetricsProducer.class.getName())
                .contains(JobRunrMetricsProducer.BackgroundJobServerMetricsProducer.class.getName());
    }

}