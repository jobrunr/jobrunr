package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
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
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
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
import org.jobrunr.quarkus.extension.deployment.AsyncJobValidator.IllegalAsyncJobAnnotationException;
import org.jobrunr.scheduling.AsyncJobInterceptor;
import org.jobrunr.scheduling.JobRunrRecurringJobRecorder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.common.NoSqlDatabaseCreator;
import org.jobrunr.utils.GraalVMUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.jobrunr.server.configuration.BackgroundJobServerThreadType.VirtualThreads;
import static org.jobrunr.utils.CollectionUtils.asSet;
import static org.jobrunr.utils.VersionNumber.JAVA_VERSION;
import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;

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
    AdditionalBeanBuildItem asyncJobInterceptor() {
        return AdditionalBeanBuildItem.unremovableOf(AsyncJobInterceptor.class);
    }

    @BuildStep
    void validateAsyncJobs(CombinedIndexBuildItem index, BuildProducer<ValidationErrorBuildItem> validationErrors) {
        try {
            AsyncJobValidator.validate(index);
        } catch (IllegalAsyncJobAnnotationException e) {
            validationErrors.produce(new ValidationErrorBuildItem(e));
        }
    }

    @BuildStep
    AdditionalBeanBuildItem produce(Capabilities capabilities, JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration) {
        Set<Class<?>> additionalBeans = new HashSet<>();
        additionalBeans.add(JobRunrProducer.class);
        additionalBeans.add(JobRunrStarter.class);
        additionalBeans.add(jsonMapper(capabilities));
        additionalBeans.addAll(storageProvider(capabilities, jobRunrBuildTimeConfiguration));

        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(additionalBeans.toArray(new Class[0]))
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem addBackgroundJobServer(JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration) {
        if (jobRunrBuildTimeConfiguration.backgroundJobServer().included()) {
            return AdditionalBeanBuildItem.builder()
                    .setUnremovable()
                    .addBeanClasses(JobRunrBackgroundJobServerProducer.class)
                    .build();
        }
        return null;
    }

    @BuildStep
    AdditionalBeanBuildItem addDashboard(JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration) {
        if (jobRunrBuildTimeConfiguration.dashboard().included()) {
            return AdditionalBeanBuildItem.builder()
                    .setUnremovable()
                    .addBeanClasses(JobRunrDashboardProducer.class)
                    .build();
        }
        return null;
    }

    @BuildStep
    public void registerRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        // Classes using java.util.Random, which need to be runtime initialized
        producer.produce(new RuntimeInitializedClassBuildItem(Job.class.getName()));
        producer.produce(new RuntimeInitializedClassBuildItem(NoSqlDatabaseCreator.class.getName()));
    }

    @BuildStep
    AdditionalBeanBuildItem addMetrics(Optional<MetricsCapabilityBuildItem> metricsCapability, JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration) {
        if (metricsCapability.isPresent() && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
            final AdditionalBeanBuildItem.Builder additionalBeanBuildItemBuilder = AdditionalBeanBuildItem.builder()
                    .setUnremovable()
                    .addBeanClasses(JobRunrMetricsStarter.class)
                    .addBeanClasses(JobRunrMetricsProducer.StorageProviderMetricsProducer.class);

            if (jobRunrBuildTimeConfiguration.backgroundJobServer().included()) {
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
    HealthBuildItem addHealthCheck(Capabilities capabilities, JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH) && jobRunrBuildTimeConfiguration.backgroundJobServer().included()) {
            return new HealthBuildItem(JobRunrHealthCheck.class.getName(), jobRunrBuildTimeConfiguration.healthEnabled());
        }
        return null;
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(DotName.createSimple(JobRequestHandler.class));
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void registerForReflection(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            CombinedIndexBuildItem indexBuildItem
    ) {
        reflectiveClassProducer.produce(ReflectiveClassBuildItem.builder(
                GraalVMUtils.JOBRUNR_CLASSES.stream().map(Class::getName).toArray(String[]::new)
        ).methods().fields().build());

        if (VirtualThreads.isSupported(JAVA_VERSION)) {
            reflectiveClassProducer.produce(ReflectiveClassBuildItem.builder(
                    Thread.class.getName(),
                    "java.lang.Thread$Builder", Executors.class.getName(), ExecutorService.class.getName()
            ).methods().fields().build());
        }

        Collection<ClassInfo> storageProviderImpls = indexBuildItem.getIndex().getAllKnownImplementors(StorageProvider.class);
        Collection<ClassInfo> jobRequestHandlerImpls = indexBuildItem.getIndex().getAllKnownImplementors(JobRequestHandler.class);
        Collection<ClassInfo> jobRequestImpls = indexBuildItem.getIndex().getAllKnownImplementors(JobRequest.class);
        Collection<ClassInfo> recurringClasses = indexBuildItem.getIndex().getAnnotations(DotName.createSimple(Recurring.class)).stream()
                .map(annotationInstance -> annotationInstance.target().asMethod().declaringClass())
                .toList();

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
            JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration
    ) {
        if (jobRunrBuildTimeConfiguration.dashboard().included()) {
            nativeImageResourceDirectoryProducer.produce(new NativeImageResourceDirectoryBuildItem("org/jobrunr/dashboard/frontend/build"));
        }

        if ("sql".equalsIgnoreCase(jobRunrBuildTimeConfiguration.database().type().orElse("sql"))) {
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
        // Unfortunately, there is no Capability.KOTLIN_SERIALIZATION.
        if (isKotlinxSerializationAndJobRunrKotlinSupportPresent()) {
            return JobRunrProducer.JobRunrKotlinxSerializataionJsonMapperProducer.class;
        }
        if (capabilities.isPresent(Capability.JACKSON)) {
            return JobRunrProducer.JobRunrJacksonJsonMapperProducer.class;
        }
        if (capabilities.isPresent(Capability.JSONB)) {
            return JobRunrProducer.JobRunrJsonBJsonMapperProducer.class;
        }
        throw new IllegalStateException("Either kotlinx.serialization + jobrunr-kotlin-x-support should be in the classpath or JSON-B/Jackson should be added as a Quarkus extension");
    }

    protected boolean isKotlinxSerializationAndJobRunrKotlinSupportPresent() {
        return classExists("kotlinx.serialization.json.Json") && classExists("org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper");
    }

    private Set<Class<?>> storageProvider(Capabilities capabilities, JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration) {
        String databaseType = jobRunrBuildTimeConfiguration.database().type().orElse(null);
        if ("sql".equalsIgnoreCase(databaseType) && !capabilities.isPresent(Capability.AGROAL)) {
            throw new IllegalStateException("You configured 'sql' as a JobRunr Database Type but the AGROAL capability is not available");
        } else if ("mongodb".equalsIgnoreCase(databaseType) && !capabilities.isPresent(Capability.MONGODB_CLIENT)) {
            throw new IllegalStateException("You configured 'mongodb' as a JobRunr Database Type but the MONGODB_CLIENT capability is not available");
        } else if ("documentdb".equalsIgnoreCase(databaseType) && !capabilities.isPresent(Capability.MONGODB_CLIENT)) {
            throw new IllegalStateException("You configured 'documentdb' as a JobRunr Database Type but the MONGODB_CLIENT capability is not available");
        }

        if (isCapabilityPresentAndConfigured(capabilities, Capability.AGROAL, "sql", databaseType)) {
            return asSet(JobRunrSqlStorageProviderProducer.class);
        } else if (isCapabilityPresentAndConfigured(capabilities, Capability.MONGODB_CLIENT, "mongodb", databaseType)) {
            return asSet(JobRunrMongoDBStorageProviderProducer.class);
        } else if (isCapabilityPresentAndConfigured(capabilities, Capability.MONGODB_CLIENT, "documentdb", databaseType)) {
            return asSet(JobRunrDocumentDBStorageProviderProducer.class);
        } else if ("mem".equalsIgnoreCase(databaseType)) {
            return asSet(JobRunrInMemoryStorageProviderProducer.class);
        }
        return Set.of();
    }

    private static boolean isCapabilityPresentAndConfigured(Capabilities capabilities, String capability, String requestedDatabaseType, String databaseType) {
        return capabilities.isPresent(capability) && (databaseType == null || requestedDatabaseType.equalsIgnoreCase(databaseType));
    }
}
