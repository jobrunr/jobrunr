package org.jobrunr.storage.nosql.elasticsearch.migrations;

import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

public class M004_CreateJobStatsIndex extends ElasticSearchMigration {

    @Override
    public void runMigration(RestHighLevelClient restHighLevelClients, String indexPrefix) throws IOException {
        //why: to be compatible with existing installations not using Migrations yet
    }
}
