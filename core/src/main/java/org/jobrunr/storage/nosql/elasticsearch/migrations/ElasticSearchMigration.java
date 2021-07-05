package org.jobrunr.storage.nosql.elasticsearch.migrations;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.jobrunr.storage.StorageException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.sleep;

public abstract class ElasticSearchMigration {

    public abstract void runMigration(RestHighLevelClient restHighLevelClients) throws IOException;

    public static void waitForHealthyCluster(RestHighLevelClient restHighLevelClient) {
        try {
            ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest();
            clusterHealthRequest.waitForYellowStatus();
            restHighLevelClient.cluster().health(clusterHealthRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    public static boolean indexExists(RestHighLevelClient client, String name) {
        waitForHealthyCluster(client);
        try {
            return client.indices().exists(new GetIndexRequest(name), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    public static void createIndex(RestHighLevelClient client, String name) {
        createIndex(client, new CreateIndexRequest(name), 0);
    }

    public static void createIndex(RestHighLevelClient client, CreateIndexRequest createIndexRequest) {
        createIndex(client, createIndexRequest, 0);
    }

    private static void createIndex(RestHighLevelClient client, CreateIndexRequest createIndexRequest, int retry) {
        sleep(retry * 500L);
        try {
            waitForHealthyCluster(client);
            client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            waitForHealthyCluster(client);
        } catch (ElasticsearchStatusException e) {
            if (e.status().getStatus() == 400) {
                if (e.getMessage().contains("resource_already_exists_exception")) {
                    // why: since we're distributed, multiple StorageProviders are trying to create indices
                    return;
                } else if (e.status().getStatus() == 400 && retry < 5) {
                    createIndex(client, createIndexRequest, retry + 1);
                } else {
                    throw new StorageException("Retried 5 times to setup ElasticSearch Indices", e);
                }
            } else {
                throw e;
            }
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    void deleteIndex(RestHighLevelClient client, String name) throws IOException {
        try {
            waitForHealthyCluster(client);
            client.indices().delete(new DeleteIndexRequest(name), RequestOptions.DEFAULT);
            waitForHealthyCluster(client);
        } catch (ElasticsearchStatusException e) {
            if (e.status().getStatus() == 404) {
                // index does not exist, so we don't have to delete it
                return;
            } else {
                throw e;
            }
        }
    }
    protected static Map<String, Object> mapping(BiConsumer<StringBuilder, Map<String, Object>>... consumers) {
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        jsonMap.put("properties", properties);

        for (BiConsumer<StringBuilder, Map<String, Object>> consumer : consumers) {
            StringBuilder sb = new StringBuilder();
            Map<String, Object> fieldProperties = new HashMap<>();
            consumer.accept(sb, fieldProperties);
            properties.put(sb.toString(), fieldProperties);

        }
        return jsonMap;
    }
}
