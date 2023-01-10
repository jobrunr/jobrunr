package org.jobrunr.storage.nosql.elasticsearch.migrations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs.FIELD_CREATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider.DEFAULT_RECURRING_JOB_INDEX_NAME;

public class M006_UpdateRecurringJobsIndex  extends ElasticSearchMigration {

    @Override
    public void runMigration(ElasticsearchClient client, String indexPrefix) throws IOException {
        final String recurringJobIndexName = elementPrefixer(indexPrefix, DEFAULT_RECURRING_JOB_INDEX_NAME);

        updateIndex(client, index(recurringJobIndexName));
    }

  private static PutMappingRequest index(String name) {
    return new PutMappingRequest.Builder()
      .index(name)
      .properties(FIELD_CREATED_AT, p -> p.long_(l -> l.index(false).store(true)))
      .build();
  }
}
