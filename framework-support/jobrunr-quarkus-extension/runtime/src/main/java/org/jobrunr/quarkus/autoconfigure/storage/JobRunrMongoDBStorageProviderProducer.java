package org.jobrunr.quarkus.autoconfigure.storage;

import com.mongodb.client.MongoClient;
import io.quarkus.arc.DefaultBean;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class JobRunrMongoDBStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(MongoClient mongoClient, JobMapper jobMapper) {
        MongoDBStorageProvider mongoDBStorageProvider = new MongoDBStorageProvider(mongoClient);
        mongoDBStorageProvider.setJobMapper(jobMapper);
        return mongoDBStorageProvider;
    }
}
