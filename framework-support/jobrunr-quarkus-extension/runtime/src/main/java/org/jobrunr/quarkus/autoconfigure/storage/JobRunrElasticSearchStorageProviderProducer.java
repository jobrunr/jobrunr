package org.jobrunr.quarkus.autoconfigure.storage;

import io.quarkus.arc.DefaultBean;
import org.elasticsearch.client.RestHighLevelClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class JobRunrElasticSearchStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(RestHighLevelClient restHighLevelClient, JobMapper jobMapper) {
        ElasticSearchStorageProvider elasticSearchStorageProvider = new ElasticSearchStorageProvider(restHighLevelClient);
        elasticSearchStorageProvider.setJobMapper(jobMapper);
        return elasticSearchStorageProvider;
    }
}
