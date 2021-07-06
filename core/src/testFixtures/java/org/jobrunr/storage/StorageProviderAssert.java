package org.jobrunr.storage;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.states.StateName;

import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

public class StorageProviderAssert extends AbstractAssert<StorageProviderAssert, StorageProvider> {

    private StorageProviderAssert(StorageProvider storageProvider) {
        super(storageProvider, StorageProviderAssert.class);
    }

    public static StorageProviderAssert assertThat(StorageProvider storageProvider) {
        return new StorageProviderAssert(storageProvider);
    }

    public StorageProviderAssert hasJobs(StateName stateName, int count) {
        Assertions.assertThat(actual.getJobPage(stateName, ascOnUpdatedAt(0)).getTotal()).isEqualTo(count);
        return this;
    }
}
