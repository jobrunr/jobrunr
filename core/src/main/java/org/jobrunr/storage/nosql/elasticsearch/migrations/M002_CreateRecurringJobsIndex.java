package org.jobrunr.storage.nosql.elasticsearch.migrations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs.FIELD_JOB_AS_JSON;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider.DEFAULT_RECURRING_JOB_INDEX_NAME;

public class M002_CreateRecurringJobsIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(ElasticsearchClient client, String indexPrefix) throws IOException {
        final String recurringJobIndexName = elementPrefixer(indexPrefix, DEFAULT_RECURRING_JOB_INDEX_NAME);
        if (indexExists(client, recurringJobIndexName))
            return; //why: to be compatible with existing installations not using Migrations yet

        createIndex(client, recurringJobIndex(recurringJobIndexName));
    }

  private static CreateIndexRequest recurringJobIndex(String name) {
    return new CreateIndexRequest.Builder()
      .index(name)
      .mappings(m -> m
        .properties(FIELD_JOB_AS_JSON, p -> p.text(t -> t.index(false).store(true)))
      )
      .build();
  }
}
