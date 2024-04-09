package org.jobrunr.storage.nosql.elasticsearch.migrations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import org.jobrunr.storage.StorageException;

import java.io.IOException;

import static co.elastic.clients.elasticsearch._types.HealthStatus.Yellow;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
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
        createIndex(client, new CreateIndexRequest.Builder().index(name).build(), 0);
    }

    public static void createIndex(ElasticsearchClient client, CreateIndexRequest request) {
        createIndex(client, request, 0);
    }

    private static void createIndex(ElasticsearchClient client, CreateIndexRequest request, int retry) {
        sleep(retry * 500L);
        try {
            waitForHealthyCluster(client);
            client.indices().create(request);
            waitForHealthyCluster(client);
        } catch (ElasticsearchException e) {
            if (e.status() == SC_BAD_REQUEST) {
                if (e.getMessage().contains("resource_already_exists_exception")) {
                    // why: since we're distributed, multiple StorageProviders are trying to create indices
                    return;
                } else if (e.status() == SC_BAD_REQUEST && retry < 5) {
                    createIndex(client, request, retry + 1);
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
            client.indices().delete(r -> r.index(name));
            waitForHealthyCluster(client);
        } catch (ElasticsearchException e) {
            if (e.status() == SC_NOT_FOUND) {
                // index does not exist, so we don't have to delete it
                return;
            }
            throw e;
        }
    }

    public static void updateIndex(ElasticsearchClient client, PutMappingRequest request) {
        try {
            waitForHealthyCluster(client);
            client.indices().putMapping(request);
            waitForHealthyCluster(client);
        } catch (ElasticsearchException e) {
            if (e.status() == SC_BAD_REQUEST) {
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
}