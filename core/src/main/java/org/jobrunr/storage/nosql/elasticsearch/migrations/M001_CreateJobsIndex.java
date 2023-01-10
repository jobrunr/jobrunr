package org.jobrunr.storage.nosql.elasticsearch.migrations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.Jobs.*;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider.DEFAULT_JOB_INDEX_NAME;

public class M001_CreateJobsIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(ElasticsearchClient client, String indexPrefix) throws IOException {
        final String jobIndexName = elementPrefixer(indexPrefix, DEFAULT_JOB_INDEX_NAME);
        if (indexExists(client, jobIndexName))
            return; //why: to be compatible with existing installations not using Migrations yet

        createIndex(client, jobIndex(jobIndexName));
    }

    private static CreateIndexRequest jobIndex(String jobIndexName) {
        return new CreateIndexRequest.Builder()
          .index(jobIndexName)
          .mappings(m -> m
            .properties(FIELD_JOB_AS_JSON, p -> p.text(t -> t.index(false).store(true)))
            .properties(FIELD_STATE, p -> p.keyword(k -> k))
            .properties(FIELD_JOB_SIGNATURE, p -> p.keyword(k -> k))
            .properties(FIELD_SCHEDULED_AT, p -> p.dateNanos(d -> d))
            .properties(FIELD_UPDATED_AT, p -> p.dateNanos(d -> d))
          )
          .build();
    }
}
