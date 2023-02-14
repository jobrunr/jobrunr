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
import org.jobrunr.quarkus.autoconfigure.storage.*;
import org.jobrunr.scheduling.JobRunrRecurringJobRecorder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.jobrunr.utils.CollectionUtils.asSet;

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
        Set<Class<?>> additionalBeans = new HashSet<>();
        additionalBeans.add(JobRunrProducer.class);
        additionalBeans.add(JobRunrStarter.class);
        additionalBeans.add(jsonMapper(capabilities));
        additionalBeans.add(JobRunrMetricsProducer.StorageProviderMetricsProducer.class);
        additionalBeans.addAll(storageProvider(capabilities));

        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(additionalBeans.toArray(new Class[0]))
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

    private Set<Class<?>> storageProvider(Capabilities capabilities) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            return asSet(JobRunrSqlStorageProviderProducer.class);
        } else if (capabilities.isPresent(Capability.MONGODB_CLIENT)) {
            return asSet(JobRunrMongoDBStorageProviderProducer.class, JobRunrDocumentDBStorageProviderProducer.class);
        } else if (capabilities.isPresent(Capability.ELASTICSEARCH_REST_HIGH_LEVEL_CLIENT)) {
            return asSet(JobRunrElasticSearchStorageProviderProducer.class);
        } else {
            return asSet(JobRunrInMemoryStorageProviderProducer.class);
        }
    }
}
