package org.jobrunr.micronaut.autoconfigure.storage;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest
class JobRunrElasticSearchStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @Test
    void elasticSearchStorageProviderAutoConfigurationTest() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(ElasticSearchStorageProvider.class)
                .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
    }

    @Singleton
    public ElasticsearchClient elasticsearchClient() throws IOException {
        return Mocks.elasticsearchClient();
    }
}