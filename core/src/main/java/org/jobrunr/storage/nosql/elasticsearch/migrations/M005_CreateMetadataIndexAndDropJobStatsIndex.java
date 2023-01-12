package org.jobrunr.storage.nosql.elasticsearch.migrations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import org.jobrunr.jobs.states.StateName;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static co.elastic.clients.elasticsearch._types.Refresh.True;
import static java.util.Optional.ofNullable;
import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider.DEFAULT_METADATA_INDEX_NAME;

public class M005_CreateMetadataIndexAndDropJobStatsIndex extends ElasticSearchMigration {
    @SuppressWarnings("unchecked")
    private static final Class<Map<Object, Object>> MAP_CLASS = (Class<Map<Object, Object>>) Map.of().getClass();

    public static final String JOBRUNR_JOB_STATS = "jobrunr_job_stats";

    @Override
    public void runMigration(
      final ElasticsearchClient client,
      final String indexPrefix) throws IOException {

        final String metadataIndexName = elementPrefixer(indexPrefix, DEFAULT_METADATA_INDEX_NAME);
        createIndex(client, metadataIndex(metadataIndexName));

        migrateExistingAllTimeSucceededFromJobStatsToMetadataAndDropJobStats(client, metadataIndexName);
    }

    private void migrateExistingAllTimeSucceededFromJobStatsToMetadataAndDropJobStats(
      final ElasticsearchClient client,
      final String metadataIndexName) throws IOException {

        long totalSucceededAmount = 0;

        if (indexExists(client, JOBRUNR_JOB_STATS)) {
            final GetResponse<Map<Object, Object>> getResponse = client.get(
              g -> g.index(JOBRUNR_JOB_STATS).id("job_stats"),
              MAP_CLASS
            );

            totalSucceededAmount = (int) ofNullable(getResponse.source())
              .orElseGet(Map::of)
              .getOrDefault(StateName.SUCCEEDED.toString(), 0L);
            deleteIndex(client, JOBRUNR_JOB_STATS);
        }

        client.index(jobStats(totalSucceededAmount, metadataIndexName));
    }

    public static IndexRequest<Map<Object, Object>> jobStats(
      final long totalSucceededAmount,
      final String metadataIndexName) {

        final Map<Object, Object> map = new LinkedHashMap<>();

        map.put(Metadata.FIELD_NAME, Metadata.STATS_NAME);
        map.put(Metadata.FIELD_OWNER, Metadata.STATS_OWNER);
        map.put(Metadata.FIELD_VALUE, totalSucceededAmount);
        map.put(Metadata.FIELD_CREATED_AT, Instant.now());
        map.put(Metadata.FIELD_UPDATED_AT, Instant.now());

        return IndexRequest.of(
          i -> i
            .index(metadataIndexName)
            .id(Metadata.STATS_ID)
            .document(map)
            .refresh(True)
        );
    }

    private static CreateIndexRequest metadataIndex(String name) {
        return new CreateIndexRequest.Builder()
          .index(name)
          .mappings(m -> m
            .properties(Metadata.FIELD_NAME, p -> p.keyword(k -> k))
            .properties(Metadata.FIELD_OWNER, p -> p.keyword(k -> k))
            .properties(Metadata.FIELD_VALUE, p -> p.text(t -> t.index(false).store(true)))
            .properties(Metadata.FIELD_CREATED_AT, p -> p.dateNanos(d -> d))
            .properties(Metadata.FIELD_UPDATED_AT, p -> p.dateNanos(d -> d))
          )
          .build();
    }
}
