package org.jobrunr.tests.e2e;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticSearchBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final ElasticsearchContainer elasticSearchContainer;

    public ElasticSearchBackgroundJobContainer(ElasticsearchContainer elasticSearch, Network network) {
        super("jobrunr-e2e-elasticsearch:1.0");
        this.elasticSearchContainer = elasticSearch;
        this.withNetwork(network);
    }

    @Override
    public void start() {
        this
                .dependsOn(elasticSearchContainer)
                .withEnv("ELASTICSEARCH_HOST", elasticSearchContainer.getNetworkAliases().get(0))
                .withEnv("ELASTICSEARCH_PORT", String.valueOf(9200));

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        return new ElasticSearchStorageProvider(elasticSearchClient());
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
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final ElasticsearchTransport transport = new RestClientTransport(http, new JacksonJsonpMapper(objectMapper));
        return new ElasticsearchClient(transport);
    }

}
