package org.jobrunr.storage.nosql.elasticsearch.migrations;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.Jobs;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.jobIndexName;

public class M001_CreateJobsIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(RestHighLevelClient client) throws IOException {
        if (indexExists(client, jobIndexName()))
            return; //why: to be compatible with existing installations not using Migrations yet

        createIndex(client, jobIndex());
    }

    private static CreateIndexRequest jobIndex() {
        return new CreateIndexRequest(jobIndexName())
                .mapping(mapping(
                        (sb, map) -> {
                            sb.append(Jobs.FIELD_JOB_AS_JSON);
                            map.put("type", "text");
                            map.put("index", false);
                            map.put("store", true);
                        },
                        (sb, map) -> {
                            sb.append(Jobs.FIELD_STATE);
                            map.put("type", "keyword");
                        },
                        (sb, map) -> {
                            sb.append(Jobs.FIELD_JOB_SIGNATURE);
                            map.put("type", "keyword");
                        },
                        (sb, map) -> {
                            sb.append(Jobs.FIELD_SCHEDULED_AT);
                            map.put("type", "date_nanos");
                        },
                        (sb, map) -> {
                            sb.append(Jobs.FIELD_UPDATED_AT);
                            map.put("type", "date_nanos");
                        }
                ));
    }
}
