package org.jobrunr.quarkus.autoconfigure.storage;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;


public class JobRunrElasticSearchStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(ElasticsearchClient elasticsearchClient, JobMapper jobMapper, JobRunrRuntimeConfiguration configuration) {
        if (configuration.database().type().isPresent() && !configuration.database().type().get().equalsIgnoreCase("elasticsearch"))
            return null;

        String tablePrefix = configuration.database().tablePrefix().orElse(null);
        DatabaseOptions databaseOptions = configuration.database().skipCreate() ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        ElasticSearchStorageProvider elasticSearchStorageProvider = new ElasticSearchStorageProvider(elasticsearchClient, tablePrefix, databaseOptions);
        elasticSearchStorageProvider.setJobMapper(jobMapper);
        return elasticSearchStorageProvider;
    }
}
