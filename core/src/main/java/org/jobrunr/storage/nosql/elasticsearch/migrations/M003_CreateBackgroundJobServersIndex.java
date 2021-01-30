package org.jobrunr.storage.nosql.elasticsearch.migrations;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jobrunr.storage.StorageProviderUtils;

import java.io.IOException;

import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.backgroundJobServerIndexName;

public class M003_CreateBackgroundJobServersIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(RestHighLevelClient client) throws IOException {
        if (indexExists(client, backgroundJobServerIndexName()))
            return; //why: to be compatible with existing installations not using Migrations yet

        createIndex(client, backgroundJobServersIndex());
    }

    private static CreateIndexRequest backgroundJobServersIndex() {
        return new CreateIndexRequest(backgroundJobServerIndexName())
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
