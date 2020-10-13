package org.jobrunr.autoconfigure.storage;

import org.elasticsearch.client.RestHighLevelClient;
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

    @Bean(name = "storageProvider")
    @ConditionalOnMissingBean
    public StorageProvider elasticSearchStorageProvider(RestHighLevelClient restHighLevelClient) {
        return new ElasticSearchStorageProvider(restHighLevelClient);
    }
}
