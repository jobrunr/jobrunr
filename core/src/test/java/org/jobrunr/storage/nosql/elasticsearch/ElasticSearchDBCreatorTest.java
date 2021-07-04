package org.jobrunr.storage.nosql.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationByClass;
import org.jobrunr.storage.nosql.elasticsearch.migrations.M001_CreateJobsIndex;
import org.jobrunr.storage.nosql.elasticsearch.migrations.M002_CreateRecurringJobsIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class ElasticSearchDBCreatorTest {

    @Container
    private static final ElasticsearchContainer elasticSearchContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.10.1").withExposedPorts(9200);

    @Mock
    private ElasticSearchStorageProvider elasticSearchStorageProviderMock;

    @Test
    void testMigrations() {
        ElasticSearchDBCreator elasticSearchDBCreator = new ElasticSearchDBCreator(elasticSearchStorageProviderMock, elasticSearchClient());

        assertThat(elasticSearchDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_CreateJobsIndex.class))).isTrue();
        assertThat(elasticSearchDBCreator.isNewMigration(new NoSqlMigrationByClass(M002_CreateRecurringJobsIndex.class))).isTrue();

        assertThatCode(elasticSearchDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(elasticSearchDBCreator::runMigrations).doesNotThrowAnyException();

        assertThat(elasticSearchDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_CreateJobsIndex.class))).isFalse();
        assertThat(elasticSearchDBCreator.isNewMigration(new NoSqlMigrationByClass(M002_CreateRecurringJobsIndex.class))).isFalse();
    }

    @Test
    void testMigrationsConcurrent() {
        ElasticSearchDBCreator elasticSearchDBCreator = new ElasticSearchDBCreator(elasticSearchStorageProviderMock, elasticSearchClient()) {
            @Override
            protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
                return true;
            }
        };

        assertThatCode(elasticSearchDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(elasticSearchDBCreator::runMigrations).doesNotThrowAnyException();
    }

    private RestHighLevelClient elasticSearchClient() {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(elasticSearchContainer.getContainerIpAddress(), elasticSearchContainer.getMappedPort(9200), "http")));

    }
}