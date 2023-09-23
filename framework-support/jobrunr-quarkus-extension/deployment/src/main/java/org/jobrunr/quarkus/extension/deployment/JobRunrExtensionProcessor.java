package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jobrunr.dashboard.ui.model.RecurringJobUIModel;
import org.jobrunr.dashboard.ui.model.VersionUIModel;
import org.jobrunr.dashboard.ui.model.problems.*;
import org.jobrunr.jobs.*;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.jobs.states.*;
import org.jobrunr.quarkus.annotations.Recurring;
import org.jobrunr.quarkus.autoconfigure.JobRunrConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrProducer;
import org.jobrunr.quarkus.autoconfigure.JobRunrStarter;
import org.jobrunr.quarkus.autoconfigure.health.JobRunrHealthCheck;
import org.jobrunr.quarkus.autoconfigure.metrics.JobRunrMetricsProducer;
import org.jobrunr.quarkus.autoconfigure.metrics.JobRunrMetricsStarter;
import org.jobrunr.quarkus.autoconfigure.storage.*;
import org.jobrunr.scheduling.JobRunrRecurringJobRecorder;
import org.jobrunr.storage.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jobrunr.jobs.context.JobDashboardLogger.JobDashboardLogLine;
import static org.jobrunr.jobs.context.JobDashboardLogger.JobDashboardLogLines;
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
    void findRecurringJobAnnotationsAndScheduleThem(RecorderContext recorderContext, CombinedIndexBuildItem index, BeanContainerBuildItem beanContainer, JobRunrRecurringJobRecorder recorder, JobRunrConfiguration jobRunrConfiguration) throws NoSuchMethodException {
        if (jobRunrConfiguration.jobScheduler.enabled) {
            new RecurringJobsFinder(recorderContext, index, beanContainer, recorder).findRecurringJobsAndScheduleThem();
        }
    }

    @BuildStep
    HealthBuildItem addHealthCheck(Capabilities capabilities, JobRunrConfiguration jobRunrConfiguration) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return new HealthBuildItem(JobRunrHealthCheck.class.getName(), jobRunrConfiguration.healthEnabled);
        }
        return null;
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(DotName.createSimple(JobRequestHandler.class));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerForReflection(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            CombinedIndexBuildItem indexBuildItem
    ) {

        reflectiveClassProducer.produce(ReflectiveClassBuildItem.builder(
                // wrapper types
                Boolean.class.getName(), Byte.class.getName(), Character.class.getName(), Float.class.getName(), Integer.class.getName(), Long.class.getName(), Short.class.getName(),
                // Java core types
                ArrayList.class.getName(), ConcurrentHashMap.class.getName(), ConcurrentLinkedQueue.class.getName(), CopyOnWriteArrayList.class.getName(), Duration.class.getName(), HashSet.class.getName(), Instant.class.getName(), UUID.class.getName(),
                // JobRunr States
                AbstractJobState.class.getName(), DeletedState.class.getName(), EnqueuedState.class.getName(), FailedState.class.getName(), JobState.class.getName(), ProcessingState.class.getName(), ScheduledState.class.getName(), StateName.class.getName(), SucceededState.class.getName(),
                // JobRunr Job
                AbstractJob.class.getName(), CachingJobDetailsGenerator.class.getName(), Job.class.getName(), JobDetails.class.getName(), JobDetailsAsmGenerator.class.getName(), JobParameter.class.getName(), RecurringJob.class.getName(),
                // JobRunr Dashboard
                BackgroundJobServerStatus.class.getName(), JobDashboardLogLine.class.getName(), JobDashboardLogLines.class.getName(), JobStats.class.getName(), JobStatsExtended.class.getName(), JobStatsExtended.Estimation.class.getName(), JobRunrMetadata.class.getName(), Page.class.getName(), PageRequest.class.getName(), RecurringJobUIModel.class.getName(), VersionUIModel.class.getName(),
                // JobRunr Dashboard Problems
                CpuAllocationIrregularityProblem.class.getName(), NewJobRunrVersionProblem.class.getName(), PollIntervalInSecondsTimeBoxIsTooSmallProblem.class.getName(), Problem.class.getName(), ScheduledJobsNotFoundProblem.class.getName(), SevereJobRunrExceptionProblem.class.getName()
        ).methods().fields().build());

        Collection<ClassInfo> storageProviderImpls = indexBuildItem.getIndex().getAllKnownImplementors(StorageProvider.class);
        Collection<ClassInfo> jobRequestHandlerImpls = indexBuildItem.getIndex().getAllKnownImplementors(JobRequestHandler.class);
        Collection<ClassInfo> jobRequestImpls = indexBuildItem.getIndex().getAllKnownImplementors(JobRequest.class);
        Collection<ClassInfo> recurringClasses = indexBuildItem.getIndex().getAnnotations(DotName.createSimple(Recurring.class)).stream()
                .map(annotationInstance -> annotationInstance.target().asMethod().declaringClass())
                .collect(Collectors.toList());

        String[] applicationClassNamesToRegister = Stream.of(storageProviderImpls.stream(), jobRequestHandlerImpls.stream(), jobRequestImpls.stream(), recurringClasses.stream())
                .flatMap(s -> s)
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);

        if (applicationClassNamesToRegister.length != 0) {
            reflectiveClassProducer.produce(ReflectiveClassBuildItem.builder(applicationClassNamesToRegister).methods().fields().build());
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerStaticResources(
            BuildProducer<NativeImageResourceDirectoryBuildItem> nativeImageResourceDirectoryProducer,
            JobRunrConfiguration jobRunrConfiguration
    ) {
        if (jobRunrConfiguration.dashboard.enabled) {
            nativeImageResourceDirectoryProducer.produce(new NativeImageResourceDirectoryBuildItem("org/jobrunr/dashboard/frontend/build"));
        }

        if ("sql".equalsIgnoreCase(jobRunrConfiguration.database.type.orElse(null))) {
            nativeImageResourceDirectoryProducer.produce(new NativeImageResourceDirectoryBuildItem("org/jobrunr/storage/sql"));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void registerLambdaCapturingTypeResources(
            CombinedIndexBuildItem indexBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResource
    ) {
        Collection<AnnotationInstance> registerForReflectionAnnotations = indexBuildItem.getIndex().getAnnotations(RegisterForReflection.class);

        for (AnnotationInstance annotationInstance : registerForReflectionAnnotations) {
            AnnotationValue lambdaCapturingTypes = annotationInstance.value("lambdaCapturingTypes");
            if (lambdaCapturingTypes != null) {
                for (AnnotationValue lambdaCapturingType : lambdaCapturingTypes.asArrayList()) {
                    String classResource = lambdaCapturingType.asString().replace('.', '/') + ".class";
                    nativeImageResource.produce(new NativeImageResourceBuildItem(classResource));
                }
            }
        }
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
