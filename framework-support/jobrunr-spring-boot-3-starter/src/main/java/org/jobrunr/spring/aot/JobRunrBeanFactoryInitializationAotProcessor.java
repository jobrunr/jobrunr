package org.jobrunr.spring.aot;

import org.jobrunr.dashboard.ui.model.problems.Problem;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.storage.nosql.mongo.migrations.MongoMigration;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.utils.GraalVMUtils;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.jobrunr.server.configuration.BackgroundJobServerThreadType.VirtualThreads;
import static org.jobrunr.utils.CollectionUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.VersionNumber.JAVA_VERSION;
import static org.jobrunr.utils.reflection.ReflectionUtils.findMethod;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;
import static org.springframework.aot.hint.MemberCategory.DECLARED_CLASSES;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

public class JobRunrBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrBeanFactoryInitializationAotProcessor.class);
    private static final MemberCategory[] allMemberCategories = MemberCategory.values();

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        boolean hasJobSchedulerOrDashboardEnabled = hasJobSchedulerOrDashboardEnabled(beanFactory);
        Set<String> recurringJobClassNames = findAllRecurringJobClassNames(beanFactory);
        Set<String> jobRequestHandlerClassNames = findAllJobRequestHandlerClassNames(beanFactory);
        if (hasJobSchedulerOrDashboardEnabled || isNotNullOrEmpty(recurringJobClassNames) || isNotNullOrEmpty(jobRequestHandlerClassNames)) {
            return (ctx, code) -> {
                var hints = ctx.getRuntimeHints();
                registerAllJobRunrClasses(hints);
                registerAllRecurringJobs(hints, recurringJobClassNames);
                registerAllJobRequestHandlers(hints, jobRequestHandlerClassNames);
            };
        }
        return null;
    }

    private boolean hasJobSchedulerOrDashboardEnabled(ConfigurableListableBeanFactory beanFactory) {
        try {
            JobRunrProperties bean = beanFactory.getBean(JobRunrProperties.class);
            return bean.getJobScheduler().isEnabled() || bean.getDashboard().isEnabled();
        } catch (BeansException e) {
            return false;
        }
    }

    private static Set<String> findAllRecurringJobClassNames(ConfigurableListableBeanFactory beanFactory) {
        String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        return stream(beanDefinitionNames)
                .map(bn -> mapBeanNameToClassName(beanFactory, bn))
                .filter(JobRunrBeanFactoryInitializationAotProcessor::isARecurringJob)
                .collect(toSet());
    }

    private static Set<String> findAllJobRequestHandlerClassNames(ConfigurableListableBeanFactory beanFactory) {
        return stream(beanFactory.getBeanNamesForType(JobRequestHandler.class))
                .map(bn -> mapBeanNameToClassName(beanFactory, bn))
                .collect(toSet());
    }

    private static void registerAllRecurringJobs(RuntimeHints hints, Set<String> recurringJobClassNames) {
        for (String clazz : recurringJobClassNames) {
            Class clazzObject = toClass(clazz);
            hints.reflection().registerType(clazzObject, allMemberCategories);
        }
    }

    private static void registerAllJobRequestHandlers(RuntimeHints hints, Set<String> jobRequestHandlerClassNames) {
        for (String clazz : jobRequestHandlerClassNames) {
            Class clazzObject = toClass(clazz);
            hints.reflection().registerType(clazzObject, allMemberCategories);
            Method runMethod = Stream.of(clazzObject.getMethods()).filter(m -> m.getName().equals("run")).toList().get(0);
            Class jobRequestType = runMethod.getParameterTypes()[0];
            hints.reflection().registerType(jobRequestType, allMemberCategories);
        }
    }

    private static void registerAllJobRunrClasses(RuntimeHints hints) {
        registerRequiredJobRunrClasses(hints);
        registerRequiredResources(hints);

        registerAllAssignableTypesOf(hints, Problem.class);
        registerAllAssignableTypesOf(hints, InMemoryStorageProvider.class);
        registerAllAssignableTypesOf(hints, SqlStorageProvider.class);
        registerAllAssignableTypesOf(hints, NoSqlMigration.class);
        registerAllAssignableTypesOf(hints, NoSqlMigrationProvider.class);

        registerNoSqlStorageProvider(hints, MongoDBStorageProvider.class, MongoMigration.class, "org/jobrunr/storage/nosql/mongo/migrations/*");
    }

    private static void registerRequiredJobRunrClasses(RuntimeHints hints) {
        ReflectionHints reflectionHints = hints.reflection();
        GraalVMUtils.JOBRUNR_CLASSES.forEach(clazz -> reflectionHints.registerType(clazz, allMemberCategories));
        if (VirtualThreads.isSupported(JAVA_VERSION)) {
            try {
                hints.reflection()
                        .registerType(Thread.class, DECLARED_CLASSES, INVOKE_PUBLIC_METHODS)
                        .registerType(Class.forName("java.lang.Thread$Builder"), allMemberCategories)
                        .registerType(Executors.class, allMemberCategories)
                        .registerType(ExecutorService.class, allMemberCategories);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Could not register virtual threads");
            }
        }
    }

    private static void registerRequiredResources(RuntimeHints hints) {
        hints.resources()
                .registerPattern("org/jobrunr/configuration/JobRunr.class")
                .registerPattern("org/jobrunr/dashboard/frontend/build/*")
                .registerPattern("org/jobrunr/storage/sql/*")
                .registerPattern("META-INF/MANIFEST.MF");
    }


    private static void registerNoSqlStorageProvider(RuntimeHints runtimeHints, Class<? extends StorageProvider> storageProviderClass, Class<?> migrationProviderClass, String pathToMigrations) {
        boolean isStorageProviderOnClasspath = registerHintByClassName(runtimeHints, storageProviderClass.getName());
        if (isStorageProviderOnClasspath) {
            registerAllAssignableTypesOf(runtimeHints, migrationProviderClass);
            runtimeHints.resources().registerPattern(pathToMigrations);
        }
    }

    private static void registerAllAssignableTypesOf(RuntimeHints runtimeHints, Class<?> anyClass) {
        Set<String> candidateClassNamesToRegister = findAllAssignableClassesOf(anyClass);
        runtimeHints.reflection().registerType(anyClass, allMemberCategories);
        LOGGER.debug("Register JobRunr class for reflection SUCCEEDED: class " + anyClass.getName() + " available for reflection in Spring Boot Native.");
        for (String candidateClassNameToRegister : candidateClassNamesToRegister) {
            registerHintByClassName(runtimeHints, candidateClassNameToRegister);
        }
    }

    private static Set<String> findAllAssignableClassesOf(Class<?> anyClass) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(anyClass));
        Set<BeanDefinition> candidateComponents = provider.findCandidateComponents("org.jobrunr");
        return candidateComponents.stream().map(BeanDefinition::getBeanClassName).collect(toSet());
    }

    private static boolean isARecurringJob(String className) {
        if (isNullOrEmpty(className))
            return false;
        if (className.startsWith("java") || className.startsWith("org.springframework") || className.startsWith("org.jobrunr"))
            return false;

        try {
            Class<?> aClass = ReflectionUtils.loadClass(className);
            return findMethod(aClass, method -> method.isAnnotationPresent(Recurring.class)).isPresent();
        } catch (ClassNotFoundException shouldNotHappen) {
            throw new IllegalStateException("Spring provided a className which is not on the classpath", shouldNotHappen);
        }
    }

    private static String mapBeanNameToClassName(ConfigurableListableBeanFactory beanFactory, String bn) {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(bn);
        if (beanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
            MethodMetadata factoryMethodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
            if (factoryMethodMetadata != null) {
                return factoryMethodMetadata.getReturnTypeName();
            }
        }
        return beanDefinition.getBeanClassName();
    }

    private static boolean registerHintByClassName(RuntimeHints runtimeHints, String className) {
        try {
            Class clazz = toClass(className);
            runtimeHints.reflection().registerType(clazz, allMemberCategories);
            LOGGER.debug("Register JobRunr class for reflection SUCCEEDED: class " + className + " available for reflection in Spring Boot Native.");
            return true;
        } catch (NoClassDefFoundError e) {
            LOGGER.debug("Register JobRunr class for reflection FAILED: Could not load class " + className + " as class dependencies (imports) are not available.");
            return false;
        }
    }
}
