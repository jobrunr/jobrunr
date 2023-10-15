package org.jobrunr.storage.nosql.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
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

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@Disabled
@ExtendWith(MockitoExtension.class)
class ElasticSearchDBCreatorTest {

    @Container
    private static final ElasticsearchContainer elasticSearchContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.10.3")
            .withEnv("ES_JAVA_OPTS", "-Xmx2048m")
            .withEnv("xpack.security.enabled", Boolean.FALSE.toString())
            .withPassword("password")
            .withExposedPorts(9200);


    @Mock
    private ElasticSearchStorageProvider elasticSearchStorageProviderMock;

    @BeforeEach
    void clearAllCollections() throws IOException {
        try {
            ElasticsearchClient elasticsearchClient = elasticSearchClient();
            elasticsearchClient.cluster().putSettings(b -> b.transient_("action.destructive_requires_name", JsonData.of(false)));
            elasticsearchClient.indices().delete(d -> d.index("_all"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "password")
        );

        final RestClient http = RestClient
                .builder(HttpHost.create(elasticSearchContainer.getHttpHostAddress()))
                .setHttpClientConfigCallback(cb -> cb.setDefaultCredentialsProvider(credentialsProvider))
                .build();
        final ElasticsearchTransport transport = new RestClientTransport(http, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}