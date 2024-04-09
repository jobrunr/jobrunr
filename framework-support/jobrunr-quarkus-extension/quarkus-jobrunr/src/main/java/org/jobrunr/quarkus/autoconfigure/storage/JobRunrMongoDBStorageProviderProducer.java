package org.jobrunr.quarkus.autoconfigure.storage;

import com.mongodb.client.MongoClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;


public class JobRunrMongoDBStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(MongoClient mongoClient, JobMapper jobMapper, JobRunrRuntimeConfiguration configuration) {
        if (configuration.database().type().isPresent() && !configuration.database().type().get().equalsIgnoreCase("mongodb")) return null;

        String databaseName = configuration.database().databaseName().orElse(null);
        String tablePrefix = configuration.database().tablePrefix().orElse(null);
        DatabaseOptions databaseOptions = configuration.database().skipCreate() ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        MongoDBStorageProvider mongoDBStorageProvider = new MongoDBStorageProvider(mongoClient, databaseName, tablePrefix, databaseOptions);
        mongoDBStorageProvider.setJobMapper(jobMapper);
        return mongoDBStorageProvider;
    }
}
