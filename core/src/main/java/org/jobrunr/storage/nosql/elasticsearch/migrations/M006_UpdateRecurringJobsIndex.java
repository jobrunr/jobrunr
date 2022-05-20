package org.jobrunr.storage.nosql.elasticsearch.migrations;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider.DEFAULT_RECURRING_JOB_INDEX_NAME;

public class M006_UpdateRecurringJobsIndex  extends ElasticSearchMigration {

    @Override
    public void runMigration(RestHighLevelClient client, String indexPrefix) throws IOException {
        final String recurringJobIndexName = elementPrefixer(indexPrefix, DEFAULT_RECURRING_JOB_INDEX_NAME);

        updateIndex(client, recurringJobIndex(recurringJobIndexName));
    }

    private static PutMappingRequest recurringJobIndex(String recurringJobIndexName) {
        return new PutMappingRequest(recurringJobIndexName)
                .source(mapping(
                        (sb, map) -> {
                            sb.append(RecurringJobs.FIELD_CREATED_AT);
                            map.put("type", "long");
                            map.put("index", false);
                            map.put("store", true);
                        }
                ));
    }
}
