package org.jobrunr.storage.nosql.elasticsearch.migrations;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageException;

import java.io.IOException;
import java.time.Instant;

import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.metadataIndexName;

public class M005_CreateMetadataIndexAndDropJobStatsIndex extends ElasticSearchMigration {

    public static final String JOBRUNR_JOB_STATS = "jobrunr_job_stats";

    @Override
    public void runMigration(RestHighLevelClient client) throws IOException {
        createIndex(client, metadataIndex());

        migrateExistingAllTimeSucceededFromJobStatsToMetadataAndDropJobStats(client);
    }

    private void migrateExistingAllTimeSucceededFromJobStatsToMetadataAndDropJobStats(RestHighLevelClient client) throws IOException {
        long totalSucceededAmount = 0;

        if (indexExists(client, JOBRUNR_JOB_STATS)) {
            GetResponse getResponse = client.get(new GetRequest(JOBRUNR_JOB_STATS, "job_stats"), RequestOptions.DEFAULT);
            totalSucceededAmount = (int) getResponse.getSource().getOrDefault(StateName.SUCCEEDED.toString(), 0L);
            deleteIndex(client, JOBRUNR_JOB_STATS);
        }

        client.index(jobStats(totalSucceededAmount), RequestOptions.DEFAULT);
    }

    public static IndexRequest jobStats(long totalSucceededAmount) {
        try {
            XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint();
            builder.startObject();
            builder.field(Metadata.FIELD_NAME, Metadata.STATS_NAME);
            builder.field(Metadata.FIELD_OWNER, Metadata.STATS_OWNER);
            builder.field(Metadata.FIELD_VALUE, totalSucceededAmount);
            builder.field(Metadata.FIELD_CREATED_AT, Instant.now());
            builder.field(Metadata.FIELD_UPDATED_AT, Instant.now());
            builder.endObject();
            return new IndexRequest(metadataIndexName())
                    .id(Metadata.STATS_ID)
                    .source(builder);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }


    private static CreateIndexRequest metadataIndex() {
        return new CreateIndexRequest(metadataIndexName())
                .mapping(mapping(
                        (sb, map) -> {
                            sb.append(Metadata.FIELD_NAME);
                            map.put("type", "keyword");
                        },
                        (sb, map) -> {
                            sb.append(Metadata.FIELD_OWNER);
                            map.put("type", "keyword");
                        },
                        (sb, map) -> {
                            sb.append(Metadata.FIELD_VALUE);
                            map.put("type", "text");
                            map.put("index", false);
                            map.put("store", true);
                        },
                        (sb, map) -> {
                            sb.append(Metadata.FIELD_CREATED_AT);
                            map.put("type", "date_nanos");
                        },
                        (sb, map) -> {
                            sb.append(Metadata.FIELD_UPDATED_AT);
                            map.put("type", "date_nanos");
                        }
                ));
    }
}
