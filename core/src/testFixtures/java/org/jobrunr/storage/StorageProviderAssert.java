package org.jobrunr.storage;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;

public class StorageProviderAssert extends AbstractAssert<StorageProviderAssert, StorageProvider> {

    private StorageProviderAssert(StorageProvider storageProvider) {
        super(storageProvider, StorageProviderAssert.class);
    }

    public static StorageProviderAssert assertThat(StorageProvider storageProvider) {
        return new StorageProviderAssert(storageProvider);
    }

    public StorageProviderAssert hasJobs(StateName stateName, int count) {
        Assertions.assertThat(actual.countJobs(stateName)).isEqualTo(count);
        return this;
    }

    public StorageProviderAssert hasJobMapper() {
        String jobMapperField = "jobMapper";
        if (MongoDBStorageProvider.class.equals(actual.getClass())) {
            jobMapperField = "jobDocumentMapper";
        } else if (ElasticSearchStorageProvider.class.equals(actual.getClass())) {
            jobMapperField = "documentMapper";
        }

        Assertions.assertThat(actual).extracting(jobMapperField).isNotNull();
        return this;
    }
}
