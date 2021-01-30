package org.jobrunr.storage.nosql.elasticsearch.migrations;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jobrunr.storage.StorageProviderUtils;

import java.io.IOException;

import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.recurringJobIndexName;

public class M002_CreateRecurringJobsIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(RestHighLevelClient client) throws IOException {
        if (indexExists(client, recurringJobIndexName()))
            return; //why: to be compatible with existing installations not using Migrations yet

        createIndex(client, recurringJobIndex());
    }

    private static CreateIndexRequest recurringJobIndex() {
        return new CreateIndexRequest(recurringJobIndexName())
                .mapping(mapping(
                        (sb, map) -> {
                            sb.append(StorageProviderUtils.RecurringJobs.FIELD_JOB_AS_JSON);
                            map.put("type", "text");
                            map.put("index", false);
                            map.put("store", true);
                        }
                ));
    }
}
