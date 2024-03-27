package org.jobrunr.spring.autoconfigure.storage;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = ElasticsearchRestClientAutoConfiguration.class, before = JobRunrAutoConfiguration.class)
@ConditionalOnBean(ElasticsearchClient.class)
@ConditionalOnProperty(prefix = "org.jobrunr.database", name = "type", havingValue = "elasticsearch", matchIfMissing = true)
public class JobRunrElasticSearchStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider elasticSearchStorageProvider(ElasticsearchClient elasticsearchClient, JobMapper jobMapper, JobRunrProperties properties) {
        String tablePrefix = properties.getDatabase().getTablePrefix();
        StorageProviderUtils.DatabaseOptions databaseOptions = properties.getDatabase().isSkipCreate() ? StorageProviderUtils.DatabaseOptions.SKIP_CREATE : StorageProviderUtils.DatabaseOptions.CREATE;
        ElasticSearchStorageProvider elasticSearchStorageProvider = new ElasticSearchStorageProvider(elasticsearchClient, tablePrefix, databaseOptions);
        elasticSearchStorageProvider.setJobMapper(jobMapper);
        return elasticSearchStorageProvider;
    }
}
