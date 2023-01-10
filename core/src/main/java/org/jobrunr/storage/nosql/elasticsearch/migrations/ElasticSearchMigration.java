package org.jobrunr.storage.nosql.elasticsearch.migrations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.jobrunr.storage.StorageException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static co.elastic.clients.elasticsearch._types.HealthStatus.Yellow;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.sleep;

public abstract class ElasticSearchMigration {

    public abstract void runMigration(ElasticsearchClient client, String indexPrefix) throws IOException;

    public static void waitForHealthyCluster(ElasticsearchClient client) {
        try {
            client
              .cluster()
              .health(r -> r.waitForStatus(Yellow));
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    public static boolean indexExists(ElasticsearchClient client, String name) {
        waitForHealthyCluster(client);
        try {
            return client
              .indices()
              .exists(r -> r.index(name))
              .value();
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    public static void createIndex(ElasticsearchClient client, String name) {
        createIndex(client, new CreateIndexRequest(name), 0);
    }

    public static void createIndex(ElasticsearchClient client, CreateIndexRequest createIndexRequest) {
        createIndex(client, createIndexRequest, 0);
    }

    private static void createIndex(ElasticsearchClient client, CreateIndexRequest createIndexRequest, int retry) {
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

    void deleteIndex(ElasticsearchClient client, String name) throws IOException {
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

    public static void updateIndex(ElasticsearchClient client, PutMappingRequest putMappingRequest) {
        try {
            waitForHealthyCluster(client);
            client.indices().putMapping(putMappingRequest, RequestOptions.DEFAULT);
            waitForHealthyCluster(client);
        } catch (ElasticsearchStatusException e) {
            if (e.status().getStatus() == 400) {
                if (e.getMessage().contains("resource_already_exists_exception")) {
                    // why: since we're distributed, multiple StorageProviders are trying to create indices
                    return;
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
