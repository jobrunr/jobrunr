package org.jobrunr.storage.nosql.elasticsearch.migrations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_FIRST_HEARTBEAT;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider.DEFAULT_BACKGROUND_JOB_SERVER_INDEX_NAME;

public class M003_CreateBackgroundJobServersIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(ElasticsearchClient client, String indexPrefix) throws IOException {
        final String backgroundJobServerIndexName = elementPrefixer(indexPrefix, DEFAULT_BACKGROUND_JOB_SERVER_INDEX_NAME);
        if (indexExists(client, backgroundJobServerIndexName))
            return; //why: to be compatible with existing installations not using Migrations yet

        createIndex(client, index(backgroundJobServerIndexName));
    }

    private static CreateIndexRequest index(String name) {
        return new CreateIndexRequest.Builder()
                .index(name)
                .mappings(m -> m
                        .properties(FIELD_FIRST_HEARTBEAT, p -> p.dateNanos(d -> d))
                        .properties(FIELD_LAST_HEARTBEAT, p -> p.dateNanos(d -> d))
                )
                .build();
    }
}