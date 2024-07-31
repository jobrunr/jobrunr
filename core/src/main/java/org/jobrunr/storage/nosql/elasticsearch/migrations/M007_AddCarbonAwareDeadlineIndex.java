package org.jobrunr.storage.nosql.elasticsearch.migrations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider.DEFAULT_CARBON_AWARE_DEADLINE_INDEX_NAME;

public class M007_AddCarbonAwareDeadlineIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(ElasticsearchClient client, String indexPrefix) throws IOException {
        final String carbonAwareDeadlineJobIndexName = elementPrefixer(indexPrefix, DEFAULT_CARBON_AWARE_DEADLINE_INDEX_NAME);

        createIndex(client, carbonAwareDeadlineJobIndexName);
    }
}