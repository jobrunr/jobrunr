package org.jobrunr.quarkus.autoconfigure.storage;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JobRunrInMemoryStorageProviderProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrInMemoryStorageProviderProducer.class);

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(JobMapper jobMapper) {
        final InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(jobMapper);
        LOGGER.warn("You're JobRunr running with the {} which is not a persisted storage. Data saved in this storage will be lost on restart.", InMemoryStorageProvider.class.getSimpleName());
        return storageProvider;
    }
}
