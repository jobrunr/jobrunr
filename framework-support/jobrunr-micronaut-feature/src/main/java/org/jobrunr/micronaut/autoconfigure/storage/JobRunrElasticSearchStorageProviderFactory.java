package org.jobrunr.micronaut.autoconfigure.storage;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration.DatabaseConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;

import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.CREATE;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.SKIP_CREATE;

@Factory
@Requires(classes = {ElasticsearchClient.class})
@Requires(beans = {ElasticsearchClient.class})
@Requires(property = "jobrunr.database.type", value = "elasticsearch", defaultValue = "elasticsearch")
public class JobRunrElasticSearchStorageProviderFactory {

    @Inject
    private JobRunrConfiguration configuration;

    @Singleton
    @Primary
    public StorageProvider elasticSearchStorageProvider(final ElasticsearchClient client,
                                                        final JobMapper jobMapper) {

        final DatabaseConfiguration db = configuration.getDatabase();
        final String prefix = db.getTablePrefix().orElse(null);
        final DatabaseOptions options = db.isSkipCreate() ? SKIP_CREATE : CREATE;
        final ElasticSearchStorageProvider provider = new ElasticSearchStorageProvider(client, prefix, options);
        provider.setJobMapper(jobMapper);

        return provider;
    }

}
