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
    AdditionalBeanBuildItem produce(Capabilities capabilities, JobRunrConfiguration jobRunrConfiguration) {
        Set<Class<?>> additionalBeans = new HashSet<>();
        additionalBeans.add(JobRunrProducer.class);
        additionalBeans.add(JobRunrStarter.class);
        additionalBeans.add(jsonMapper(capabilities));
        additionalBeans.add(JobRunrMetricsProducer.StorageProviderMetricsProducer.class);
        additionalBeans.addAll(storageProvider(capabilities, jobRunrConfiguration));

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

    private Set<Class<?>> storageProvider(Capabilities capabilities, JobRunrConfiguration jobRunrConfiguration) {
        String databaseType = jobRunrConfiguration.database.type.orElse(null);
        if ("sql".equalsIgnoreCase(databaseType) && !capabilities.isPresent(Capability.AGROAL)) {
            throw new IllegalStateException("You configured 'sql' as a JobRunr Database Type but the AGROAL capability is not available");
        } else if ("mongodb".equalsIgnoreCase(databaseType) && !capabilities.isPresent(Capability.MONGODB_CLIENT)) {
            throw new IllegalStateException("You configured 'mongodb' as a JobRunr Database Type but the MONGODB_CLIENT capability is not available");
        } else if ("documentdb".equalsIgnoreCase(databaseType) && !capabilities.isPresent(Capability.MONGODB_CLIENT)) {
            throw new IllegalStateException("You configured 'documentdb' as a JobRunr Database Type but the MONGODB_CLIENT capability is not available");
        } else if ("elasticsearch".equalsIgnoreCase(databaseType) && !capabilities.isPresent(Capability.ELASTICSEARCH_REST_HIGH_LEVEL_CLIENT)) {
            throw new IllegalStateException("You configured 'elasticsearch' as a JobRunr Database Type but the ELASTICSEARCH_REST_HIGH_LEVEL_CLIENT capability is not available");
        }

        if (isCapabilityPresentAndConfigured(capabilities, Capability.AGROAL, "sql", databaseType)) {
            return asSet(JobRunrSqlStorageProviderProducer.class);
        } else if (isCapabilityPresentAndConfigured(capabilities, Capability.MONGODB_CLIENT, "mongodb", databaseType)) {
            return asSet(JobRunrMongoDBStorageProviderProducer.class);
        } else if (isCapabilityPresentAndConfigured(capabilities, Capability.MONGODB_CLIENT, "documentdb", databaseType)) {
            return asSet(JobRunrDocumentDBStorageProviderProducer.class);
        } else if (isCapabilityPresentAndConfigured(capabilities, Capability.ELASTICSEARCH_REST_HIGH_LEVEL_CLIENT, "elasticsearch", databaseType)) {
            return asSet(JobRunrElasticSearchStorageProviderProducer.class);
        } else {
            return asSet(JobRunrInMemoryStorageProviderProducer.class);
        }
    }

    private static boolean isCapabilityPresentAndConfigured(Capabilities capabilities, String capability, String requestedDatabaseType, String databaseType) {
        return capabilities.isPresent(capability) && (databaseType == null || requestedDatabaseType.equalsIgnoreCase(databaseType));
    }
}
