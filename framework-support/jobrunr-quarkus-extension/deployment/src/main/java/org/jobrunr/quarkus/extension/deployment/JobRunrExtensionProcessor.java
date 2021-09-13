package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
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
import org.jobrunr.scheduling.JobRunrRecurringJobRecorder;

import java.util.Optional;

/**
 * Class responsible for creating additional JobRunr beans in Quarkus.
 */
class JobRunrExtensionProcessor {

    private static final String FEATURE = "jobrunr";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem produce(Capabilities capabilities) {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(
                        JobRunrProducer.class,
                        JobRunrStarter.class,
                        storageProvider(capabilities),
                        jsonMapper(capabilities),
                        JobRunrMetricsProducer.StorageProviderMetricsProducer.class
                )
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem addMetrics(Optional<MetricsCapabilityBuildItem> metricsCapability, JobRunrConfiguration jobRunrConfiguration) {
        if (metricsCapability.isPresent() && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
            final AdditionalBeanBuildItem.Builder additionalBeanBuildItemBuilder = AdditionalBeanBuildItem.builder()
                    .setUnremovable()
                    .addBeanClasses(JobRunrMetricsStarter.class)
                    .addBeanClasses(JobRunrMetricsProducer.StorageProviderMetricsProducer.class);

            if (jobRunrConfiguration.backgroundJobServer.enabled) {
                additionalBeanBuildItemBuilder.addBeanClasses(JobRunrMetricsProducer.BackgroundJobServerMetricsProducer.class);
            }
            return additionalBeanBuildItemBuilder
                    .build();
        }
        return null;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void findRecurringJobAnnotationsAndScheduleThem(RecorderContext recorderContext, CombinedIndexBuildItem index, BeanContainerBuildItem beanContainer, JobRunrRecurringJobRecorder recorder) throws NoSuchMethodException {
        new RecurringJobsFinder(recorderContext, index, beanContainer, recorder).findRecurringJobsAndScheduleThem();
    }

    @BuildStep
    HealthBuildItem addHealthCheck(Capabilities capabilities, JobRunrConfiguration jobRunrConfiguration) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return new HealthBuildItem(JobRunrHealthCheck.class.getName(), jobRunrConfiguration.healthEnabled);
        }
        return null;
    }


    private Class<?> jsonMapper(Capabilities capabilities) {
        if (capabilities.isPresent(Capability.JSONB)) {
            return JobRunrProducer.JobRunrJsonBJsonMapperProducer.class;
        } else if (capabilities.isPresent(Capability.JACKSON)) {
            return JobRunrProducer.JobRunrJacksonJsonMapperProducer.class;
        }
        throw new IllegalStateException("Either JSON-B or Jackson should be added via a Quarkus extension");
    }

    private Class<?> storageProvider(Capabilities capabilities) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            return JobRunrSqlStorageProviderProducer.class;
        } else if (capabilities.isPresent(Capability.MONGODB_CLIENT)) {
            return JobRunrMongoDBStorageProviderProducer.class;
        } else if (capabilities.isPresent(Capability.ELASTICSEARCH_REST_HIGH_LEVEL_CLIENT)) {
            return JobRunrElasticSearchStorageProviderProducer.class;
        } else {
            return JobRunrInMemoryStorageProviderProducer.class;
        }
    }
}
