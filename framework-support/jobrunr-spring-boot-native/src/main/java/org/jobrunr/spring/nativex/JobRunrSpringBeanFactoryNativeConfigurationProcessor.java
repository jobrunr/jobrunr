package org.jobrunr.spring.nativex;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.nosql.documentdb.AmazonDocumentDBStorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.storage.nosql.redis.JedisRedisStorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.db2.DB2StorageProvider;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.jobrunr.storage.sql.mariadb.MariaDbStorageProvider;
import org.jobrunr.storage.sql.oracle.OracleStorageProvider;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.jobrunr.storage.sql.sqlite.SqLiteStorageProvider;
import org.jobrunr.storage.sql.sqlserver.SQLServerStorageProvider;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.BeanFactoryNativeConfigurationProcessor;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.nativex.hint.TypeAccess;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class JobRunrSpringBeanFactoryNativeConfigurationProcessor implements BeanFactoryNativeConfigurationProcessor {

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

        registry.reflection().forType(AmazonDocumentDBStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(DB2StorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(DefaultSqlStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(ElasticSearchStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(H2StorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(InMemoryStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(JedisRedisStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(LettuceRedisStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(MariaDbStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(MongoDBStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(PostgresStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(OracleStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(SQLServerStorageProvider.class).withAccess(TypeAccess.values()).build();
        registry.reflection().forType(SqLiteStorageProvider.class).withAccess(TypeAccess.values()).build();
    }
}
