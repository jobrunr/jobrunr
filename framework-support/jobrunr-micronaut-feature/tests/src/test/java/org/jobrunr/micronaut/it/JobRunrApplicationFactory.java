package org.jobrunr.micronaut.it;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

@Factory
public class JobRunrApplicationFactory {

    @Singleton
    StorageProvider storageProvider(JsonMapper jsonMapper) {
        StorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(jsonMapper));
        return storageProvider;
    }
}
