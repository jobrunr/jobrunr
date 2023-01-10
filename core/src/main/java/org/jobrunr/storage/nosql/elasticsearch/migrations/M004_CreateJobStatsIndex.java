package org.jobrunr.storage.nosql.elasticsearch.migrations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import java.io.IOException;

public class M004_CreateJobStatsIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(ElasticsearchClient client, String indexPrefix) throws IOException {
        //why: to be compatible with existing installations not using Migrations yet
    }
}
