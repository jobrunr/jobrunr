package org.jobrunr.storage.nosql.elasticsearch.migrations;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jobrunr.storage.StorageProviderUtils;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider.DEFAULT_RECURRING_JOB_INDEX_NAME;

public class M002_CreateRecurringJobsIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(RestHighLevelClient client, String indexPrefix) throws IOException {
        final String recurringJobIndexName = elementPrefixer(indexPrefix, DEFAULT_RECURRING_JOB_INDEX_NAME);
        if (indexExists(client, recurringJobIndexName))
            return; //why: to be compatible with existing installations not using Migrations yet

        createIndex(client, recurringJobIndex(recurringJobIndexName));
    }

    private static CreateIndexRequest recurringJobIndex(String recurringJobIndexName) {
        return new CreateIndexRequest(recurringJobIndexName)
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
