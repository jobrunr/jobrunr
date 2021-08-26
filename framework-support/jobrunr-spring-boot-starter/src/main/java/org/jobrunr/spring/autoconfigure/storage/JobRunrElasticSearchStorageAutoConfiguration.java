package org.jobrunr.spring.autoconfigure.storage;

import org.elasticsearch.client.RestHighLevelClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(RestHighLevelClient.class)
@AutoConfigureAfter(ElasticsearchRestClientAutoConfiguration.class)
public class JobRunrElasticSearchStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider elasticSearchStorageProvider(RestHighLevelClient restHighLevelClient, JobMapper jobMapper) {
        ElasticSearchStorageProvider elasticSearchStorageProvider = new ElasticSearchStorageProvider(restHighLevelClient);
        elasticSearchStorageProvider.setJobMapper(jobMapper);
        return elasticSearchStorageProvider;
    }
}
