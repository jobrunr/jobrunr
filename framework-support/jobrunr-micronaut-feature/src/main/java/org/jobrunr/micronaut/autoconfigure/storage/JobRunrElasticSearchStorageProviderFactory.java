package org.jobrunr.micronaut.autoconfigure.storage;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;

@Factory
@Requires(classes = {ElasticsearchClient.class})
@Requires(beans = {ElasticsearchClient.class})
@Requires(property = "jobrunr.database.type", value = "elasticsearch", defaultValue = "elasticsearch")
public class JobRunrElasticSearchStorageProviderFactory {

    @Inject
    private JobRunrConfiguration configuration;

    @Singleton
    @Primary
    public StorageProvider elasticSearchStorageProvider(ElasticsearchClient elasticsearchClient, JobMapper jobMapper) {
        String tablePrefix = configuration.getDatabase().getTablePrefix().orElse(null);
        StorageProviderUtils.DatabaseOptions databaseOptions = configuration.getDatabase().isSkipCreate() ? StorageProviderUtils.DatabaseOptions.SKIP_CREATE : StorageProviderUtils.DatabaseOptions.CREATE;
        ElasticSearchStorageProvider elasticSearchStorageProvider = new ElasticSearchStorageProvider(elasticsearchClient);
        new ElasticSearchStorageProvider(elasticsearchClient, tablePrefix, databaseOptions);
        elasticSearchStorageProvider.setJobMapper(jobMapper);
        return elasticSearchStorageProvider;
    }

}
