package org.jobrunr.spring.autoconfigure.storage;

import com.mongodb.client.MongoClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.documentdb.AmazonDocumentDBStorageProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = MongoAutoConfiguration.class, before = JobRunrAutoConfiguration.class)
@ConditionalOnBean(MongoClient.class)
@ConditionalOnProperty(prefix = "jobrunr.database", name = "type", havingValue = "documentdb", matchIfMissing = false)
public class JobRunrDocumentDBStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider documentDBStorageProvider(MongoClient mongoClient, JobMapper jobMapper, JobRunrProperties properties) {
        String databaseName = properties.getDatabase().getDatabaseName();
        String tablePrefix = properties.getDatabase().getTablePrefix();
        DatabaseOptions databaseOptions = properties.getDatabase().isSkipCreate() ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        AmazonDocumentDBStorageProvider amazonDocumentDBStorageProvider = new AmazonDocumentDBStorageProvider(mongoClient, databaseName, tablePrefix, databaseOptions);
        amazonDocumentDBStorageProvider.setJobMapper(jobMapper);
        return amazonDocumentDBStorageProvider;
    }
}
