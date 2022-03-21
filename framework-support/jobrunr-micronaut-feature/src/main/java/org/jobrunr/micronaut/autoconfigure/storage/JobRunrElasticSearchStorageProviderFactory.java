package org.jobrunr.micronaut.autoconfigure.storage;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.elasticsearch.client.RestHighLevelClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;

@Factory
@Requires(classes = {RestHighLevelClient.class})
@Requires(beans = {RestHighLevelClient.class})
@Requires(property = "jobrunr.database.type", value = "elasticsearch", defaultValue = "elasticsearch")
public class JobRunrElasticSearchStorageProviderFactory {

    @Inject
    private JobRunrConfiguration configuration;

    @Singleton
    @Primary
    public StorageProvider elasticSearchStorageProvider(RestHighLevelClient restHighLevelClient, JobMapper jobMapper) {
        String tablePrefix = configuration.getDatabase().getTablePrefix().orElse(null);
        StorageProviderUtils.DatabaseOptions databaseOptions = configuration.getDatabase().isSkipCreate() ? StorageProviderUtils.DatabaseOptions.SKIP_CREATE : StorageProviderUtils.DatabaseOptions.CREATE;
        org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider elasticSearchStorageProvider = new org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider(restHighLevelClient, tablePrefix, databaseOptions);
        elasticSearchStorageProvider.setJobMapper(jobMapper);
        return elasticSearchStorageProvider;
    }

}
