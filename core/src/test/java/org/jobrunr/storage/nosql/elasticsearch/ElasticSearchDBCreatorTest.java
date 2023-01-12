package org.jobrunr.storage.nosql.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.jobrunr.JobRunrException;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationByClass;
import org.jobrunr.storage.nosql.elasticsearch.migrations.M001_CreateJobsIndex;
import org.jobrunr.storage.nosql.elasticsearch.migrations.M002_CreateRecurringJobsIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static java.util.Map.of;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
@Disabled
@ExtendWith(MockitoExtension.class)
class ElasticSearchDBCreatorTest {

    @Container
    private static final ElasticsearchContainer elasticSearchContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.8")
      .withEnv(of(
        "ES_JAVA_OPTS", "-Xmx512m",
        "discovery.type", "single-node",
        "xpack.security.enabled", "false"
      )).withExposedPorts(9200);

    @Mock
    private ElasticSearchStorageProvider elasticSearchStorageProviderMock;

    @BeforeEach
    void clearAllCollections() throws IOException {
        elasticSearchClient().indices().delete(d -> d.index("_all"));
    }

    @Test
    void testMigrations() throws IOException {
        ElasticSearchDBCreator elasticSearchDBCreator = new ElasticSearchDBCreator(elasticSearchStorageProviderMock, elasticSearchClient(), null);

        assertThat(elasticSearchDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_CreateJobsIndex.class))).isTrue();
        assertThat(elasticSearchDBCreator.isNewMigration(new NoSqlMigrationByClass(M002_CreateRecurringJobsIndex.class))).isTrue();

        assertThatCode(elasticSearchDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(elasticSearchDBCreator::runMigrations).doesNotThrowAnyException();

        assertThat(elasticSearchDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_CreateJobsIndex.class))).isFalse();
        assertThat(elasticSearchDBCreator.isNewMigration(new NoSqlMigrationByClass(M002_CreateRecurringJobsIndex.class))).isFalse();
        assertThat(elasticSearchClient().indices().get(g -> g.index("*")).result()).hasSize(5);
    }

    @Test
    void testValidateIndicesNoIndexPrefix() throws IOException {
        ElasticSearchDBCreator elasticSearchDBCreator = new ElasticSearchDBCreator(elasticSearchStorageProviderMock, elasticSearchClient(), null);
        assertThatThrownBy(elasticSearchDBCreator::validateIndices)
                .isInstanceOf(JobRunrException.class)
                .hasMessage("Not all required indices are available by JobRunr!");

        elasticSearchDBCreator.runMigrations();

        assertThatCode(elasticSearchDBCreator::validateIndices).doesNotThrowAnyException();
        assertThat(elasticSearchClient().indices().get(g -> g.index("*")).result()).hasSize(5);
    }

    @Test
    void testValidateIndicesWithIndexPrefix() {
        ElasticSearchDBCreator elasticSearchDBCreator = new ElasticSearchDBCreator(elasticSearchStorageProviderMock, elasticSearchClient(), "my_index_prefix_");
        assertThatThrownBy(elasticSearchDBCreator::validateIndices)
                .isInstanceOf(JobRunrException.class)
                .hasMessage("Not all required indices are available by JobRunr!");

        elasticSearchDBCreator.runMigrations();

        assertThatCode(elasticSearchDBCreator::validateIndices).doesNotThrowAnyException();
    }


    @Test
    void testMigrationsConcurrent() {
        ElasticSearchDBCreator elasticSearchDBCreator = new ElasticSearchDBCreator(elasticSearchStorageProviderMock, elasticSearchClient(), null) {
            @Override
            protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
                return true;
            }
        };

        assertThatCode(elasticSearchDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(elasticSearchDBCreator::runMigrations).doesNotThrowAnyException();
    }

    private ElasticsearchClient elasticSearchClient() {
        final RestClient http = RestClient.builder(
          new HttpHost(elasticSearchContainer.getHost(), elasticSearchContainer.getMappedPort(9200), "http"))
          .build();
        final ElasticsearchTransport transport = new RestClientTransport(http, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}