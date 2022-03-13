package org.jobrunr.spring.nativex;

import org.jobrunr.dashboard.ui.model.problems.Problem;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationProvider;
import org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration;
import org.jobrunr.storage.nosql.mongo.migrations.MongoMigration;
import org.jobrunr.storage.nosql.redis.migrations.JedisRedisMigration;
import org.jobrunr.storage.nosql.redis.migrations.LettuceRedisMigration;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.BeanFactoryNativeConfigurationProcessor;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.nativex.hint.TypeAccess;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class JobRunrSpringBeanFactoryNativeConfigurationProcessor implements BeanFactoryNativeConfigurationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrSpringBeanFactoryNativeConfigurationProcessor.class);

    @Override
    public void process(ConfigurableListableBeanFactory beanFactory, NativeConfigurationRegistry registry) {
        try {
            String[] jobRequestHandlers = beanFactory.getBeanNamesForType(JobRequestHandler.class);
            for (String beanName : jobRequestHandlers) {
                BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                String clazz = bd.getBeanClassName();

                Class clazzObject = ReflectionUtils.loadClass(clazz);
                registry.reflection().forType(clazzObject).withAccess(TypeAccess.values()).build();
                Method runMethod = Stream.of(clazzObject.getMethods()).filter(m -> m.getName().equals("run")).collect(toList()).get(0);
                Class jobRequestType = runMethod.getParameterTypes()[0];
                registry.reflection().forType(jobRequestType).withAccess(TypeAccess.values()).build();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not enhance JobRequestHandlers for Spring Boot Native", e);
        }

        registerAllAssignableTypesOf(registry, Problem.class);
        registerAllAssignableTypesOf(registry, StorageProvider.class);
        registerAllAssignableTypesOf(registry, NoSqlMigration.class);
        registerAllAssignableTypesOf(registry, NoSqlMigrationProvider.class);
        // do not forget to paths to migrations as resource hints
        registerAllAssignableTypesOf(registry, ElasticSearchMigration.class);
        registerAllAssignableTypesOf(registry, MongoMigration.class);
        registerAllAssignableTypesOf(registry, JedisRedisMigration.class);
        registerAllAssignableTypesOf(registry, LettuceRedisMigration.class);
    }

    private void registerAllAssignableTypesOf(NativeConfigurationRegistry registry, Class<?> anyClass) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(anyClass));
        Set<BeanDefinition> candidateComponents = provider.findCandidateComponents("org.jobrunr");
        registry.reflection().forType(anyClass).withAccess(TypeAccess.values()).build();
        LOGGER.info("Register JobRunr class for reflection SUCCEEDED: class " + anyClass.getName() + " available for reflection in Spring Boot Native.");
        for (BeanDefinition beanDefinition : candidateComponents) {
            try {
                Class storageProviderImplementation = ReflectionUtils.toClass(beanDefinition.getBeanClassName());
                registry.reflection().forType(storageProviderImplementation).withAccess(TypeAccess.values()).build();
                LOGGER.info("Register JobRunr class for reflection SUCCEEDED: class " + beanDefinition.getBeanClassName() + " available for reflection in Spring Boot Native.");
            } catch (NoClassDefFoundError e) {
                LOGGER.info("Register JobRunr class for reflection FAILED: Could not load class " + beanDefinition.getBeanClassName() + " as class dependencies (imports) are not available.");
            }
        }
    }
}
