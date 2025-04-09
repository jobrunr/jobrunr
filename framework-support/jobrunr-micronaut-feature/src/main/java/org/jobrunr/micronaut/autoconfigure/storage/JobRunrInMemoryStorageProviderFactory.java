package org.jobrunr.micronaut.autoconfigure.storage;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Factory
@Requires(property = "jobrunr.database.type", value = "mem")
public class JobRunrInMemoryStorageProviderFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrInMemoryStorageProviderFactory.class);

    @Singleton
    @Primary
    public StorageProvider sqlStorageProvider(JobMapper jobMapper) {
        final InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(jobMapper);
        LOGGER.warn("You're JobRunr running with the {} which is not a persisted storage. Data saved in this storage will be lost on restart.", InMemoryStorageProvider.class.getSimpleName());
        return storageProvider;
    }
}
