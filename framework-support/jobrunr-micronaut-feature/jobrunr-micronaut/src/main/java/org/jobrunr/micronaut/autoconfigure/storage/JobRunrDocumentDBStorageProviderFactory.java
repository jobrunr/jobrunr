package org.jobrunr.micronaut.autoconfigure.storage;

import com.mongodb.client.MongoClient;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.nosql.documentdb.AmazonDocumentDBStorageProvider;

@Factory
@Requires(classes = {MongoClient.class})
@Requires(beans = {MongoClient.class})
@Requires(property = "jobrunr.database.type", value = "documentdb")
public class JobRunrDocumentDBStorageProviderFactory {

    @Inject
    private JobRunrConfiguration configuration;

    @Singleton
    @Primary
    public StorageProvider mongoDBStorageProvider(MongoClient mongoClient, JobMapper jobMapper) {
        String databaseName = configuration.getDatabase().getDatabaseName().orElse(null);
        String tablePrefix = configuration.getDatabase().getTablePrefix().orElse(null);
        StorageProviderUtils.DatabaseOptions databaseOptions = configuration.getDatabase().isSkipCreate() ? StorageProviderUtils.DatabaseOptions.SKIP_CREATE : StorageProviderUtils.DatabaseOptions.CREATE;
        AmazonDocumentDBStorageProvider amazonDocumentDBStorageProvider = new AmazonDocumentDBStorageProvider(mongoClient, databaseName, tablePrefix, databaseOptions);
        amazonDocumentDBStorageProvider.setJobMapper(jobMapper);
        return amazonDocumentDBStorageProvider;
    }
}
