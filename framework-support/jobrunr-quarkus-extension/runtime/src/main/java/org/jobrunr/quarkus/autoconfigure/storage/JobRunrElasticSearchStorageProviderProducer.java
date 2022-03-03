package org.jobrunr.quarkus.autoconfigure.storage;

import io.quarkus.arc.DefaultBean;
import org.elasticsearch.client.RestHighLevelClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.quarkus.autoconfigure.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class JobRunrElasticSearchStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(RestHighLevelClient restHighLevelClient, JobMapper jobMapper, JobRunrConfiguration configuration) {
        if(configuration.database.type.isPresent() && !configuration.database.type.get().equalsIgnoreCase("elasticsearch")) return null;

        String tablePrefix = configuration.database.tablePrefix.orElse(null);
        DatabaseOptions databaseOptions = configuration.database.skipCreate ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        ElasticSearchStorageProvider elasticSearchStorageProvider = new ElasticSearchStorageProvider(restHighLevelClient, tablePrefix, databaseOptions);
        elasticSearchStorageProvider.setJobMapper(jobMapper);
        return elasticSearchStorageProvider;
    }
}
