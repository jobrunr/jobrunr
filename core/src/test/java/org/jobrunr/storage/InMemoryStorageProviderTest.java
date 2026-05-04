package org.jobrunr.storage;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public class InMemoryStorageProviderTest extends StorageProviderTest {

    @Override
    protected void cleanup(int testMethodIndex) {
        storageProvider = new InMemoryStorageProvider(rateLimit().withoutLimits());
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
    }

    @Override
    protected StorageProvider getStorageProvider() {
        return storageProvider;
    }
}
