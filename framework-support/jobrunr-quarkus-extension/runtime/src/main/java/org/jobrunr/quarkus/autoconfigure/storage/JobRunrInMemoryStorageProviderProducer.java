package org.jobrunr.quarkus.autoconfigure.storage;

import io.quarkus.arc.DefaultBean;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class JobRunrInMemoryStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    //TODO: make it return correct storageprovider based on Extensions (mongo, redis, elasticsearch)?
    public StorageProvider storageProvider(JobMapper jobMapper) {
        final InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }
}
