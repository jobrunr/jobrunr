package org.jobrunr.quarkus.autoconfigure.storage;

import com.mongodb.client.MongoClient;
import io.quarkus.arc.DefaultBean;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.quarkus.autoconfigure.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.documentdb.AmazonDocumentDBStorageProvider;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class JobRunrDocumentDBStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(MongoClient mongoClient, JobMapper jobMapper, JobRunrConfiguration configuration) {
        if (!configuration.database.type.isPresent() || !configuration.database.type.get().equalsIgnoreCase("documentdb")) return null;
        
        String databaseName = configuration.database.databaseName.orElse(null);
        String tablePrefix = configuration.database.tablePrefix.orElse(null);
        DatabaseOptions databaseOptions = configuration.database.skipCreate ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        AmazonDocumentDBStorageProvider amazonDocumentDBStorageProvider = new AmazonDocumentDBStorageProvider(mongoClient, databaseName, tablePrefix, databaseOptions);
        amazonDocumentDBStorageProvider.setJobMapper(jobMapper);
        return amazonDocumentDBStorageProvider;
    }
}
