package org.jobrunr.spring.autoconfigure.storage;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.CREATE;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.SKIP_CREATE;

@Configuration
@ConditionalOnBean(ElasticsearchClient.class)
@AutoConfigureAfter(ElasticsearchRestClientAutoConfiguration.class)
@ConditionalOnProperty(prefix = "org.jobrunr.database", name = "type", havingValue = "elasticsearch", matchIfMissing = true)
public class JobRunrElasticSearchStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider elasticSearchStorageProvider(
      final ElasticsearchClient client,
      final JobMapper mapper,
      final JobRunrProperties properties) {

        final String prefix = properties.getDatabase().getTablePrefix();
        final DatabaseOptions databaseOptions = properties.getDatabase().isSkipCreate() ? SKIP_CREATE : CREATE;

        final ElasticSearchStorageProvider provider = new ElasticSearchStorageProvider(client, prefix, databaseOptions);
        provider.setJobMapper(mapper);

        return provider;
    }
}
