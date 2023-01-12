package org.jobrunr.quarkus.autoconfigure.storage;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.quarkus.arc.DefaultBean;
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
    public StorageProvider storageProvider(
      final ElasticsearchClient client,
      final JobMapper jobMapper,
      final JobRunrConfiguration configuration) {

        if (configuration.database.type.isPresent() && !configuration.database.type.get().equalsIgnoreCase("elasticsearch")) return null;

        final String tableprefix = configuration.database.tablePrefix.orElse(null);
        final DatabaseOptions options = configuration.database.skipCreate ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        final ElasticSearchStorageProvider provider = new ElasticSearchStorageProvider(client, tableprefix, options);
        provider.setJobMapper(jobMapper);

        return provider;
    }
}
