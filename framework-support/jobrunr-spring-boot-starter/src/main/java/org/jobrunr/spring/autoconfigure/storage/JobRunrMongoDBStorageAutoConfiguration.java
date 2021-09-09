package org.jobrunr.spring.autoconfigure.storage;

import com.mongodb.client.MongoClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(MongoClient.class)
@AutoConfigureAfter(MongoAutoConfiguration.class)
public class JobRunrMongoDBStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider mongoDBStorageProvider(MongoClient mongoClient, JobMapper jobMapper, JobRunrProperties properties) {
        String databaseName = properties.getDatabase().getDatabaseName();
        String tablePrefix = properties.getDatabase().getTablePrefix();
        DatabaseOptions databaseOptions = properties.getDatabase().isSkipCreate() ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        MongoDBStorageProvider mongoDBStorageProvider = new MongoDBStorageProvider(mongoClient, databaseName, tablePrefix, databaseOptions);
        mongoDBStorageProvider.setJobMapper(jobMapper);
        return mongoDBStorageProvider;
    }
}
