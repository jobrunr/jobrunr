package org.jobrunr.storage.nosql.elasticsearch.migrations;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jobrunr.storage.StorageProviderUtils;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider.DEFAULT_BACKGROUND_JOB_SERVER_INDEX_NAME;

public class M003_CreateBackgroundJobServersIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(RestHighLevelClient client, String indexPrefix) throws IOException {
        final String backgroundJobServerIndexName = elementPrefixer(indexPrefix, DEFAULT_BACKGROUND_JOB_SERVER_INDEX_NAME);
        if (indexExists(client, backgroundJobServerIndexName))
            return; //why: to be compatible with existing installations not using Migrations yet

        createIndex(client, backgroundJobServersIndex(backgroundJobServerIndexName));
    }

    private static CreateIndexRequest backgroundJobServersIndex(String backgroundJobServerIndexName) {
        return new CreateIndexRequest(backgroundJobServerIndexName)
                .mapping(mapping(
                        (sb, map) -> {
                            sb.append(StorageProviderUtils.BackgroundJobServers.FIELD_FIRST_HEARTBEAT);
                            map.put("type", "date_nanos");
                        },
                        (sb, map) -> {
                            sb.append(StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT);
                            map.put("type", "date_nanos");
                        }
                ));
    }
}
