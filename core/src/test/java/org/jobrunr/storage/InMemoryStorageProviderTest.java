package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

public class InMemoryStorageProviderTest extends StorageProviderTest {

    private StorageProvider storageProvider;

    @Override
    protected void cleanup() {
        storageProvider = new InMemoryStorageProvider(rateLimit().withoutLimits());
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
    }

    @Override
    protected StorageProvider getStorageProvider() {
        return storageProvider;
    }

    @Override
    protected ThrowingStorageProvider makeThrowingStorageProvider(StorageProvider storageProvider) {
        return new ThrowingInMemoryStorageProvider(storageProvider);
    }

    public class ThrowingInMemoryStorageProvider extends ThrowingStorageProvider {

        private Map<UUID, Job> originalJobQueue;

        public ThrowingInMemoryStorageProvider(StorageProvider storageProvider) {
            super(storageProvider, "jobQueue");
        }

        @Override
        protected void makeStorageProviderThrowException(StorageProvider storageProvider) {
            Map<UUID, Job> jobQueue = Mockito.mock(Map.class);
            when(jobQueue.put(Mockito.any(), Mockito.any())).thenThrow(new StorageException("Boem!"));
            setInternalState(storageProvider, "jobQueue", jobQueue);
        }
    }
}
