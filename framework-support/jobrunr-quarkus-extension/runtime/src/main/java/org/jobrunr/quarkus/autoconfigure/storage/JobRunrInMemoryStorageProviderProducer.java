package org.jobrunr.quarkus.autoconfigure.storage;

import io.quarkus.arc.DefaultBean;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class JobRunrInMemoryStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    //TODO: make it return correct storageprovider based on Extensions (mongo, redis, elasticsearch)?
    public StorageProvider storageProvider() {
        return new InMemoryStorageProvider();
    }
}
