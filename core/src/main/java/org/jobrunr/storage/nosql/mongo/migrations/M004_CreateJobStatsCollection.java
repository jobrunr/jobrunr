package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoDatabase;

public class M004_CreateJobStatsCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase) {
        //why: to be compatible with existing installations not using Migrations yet
    }
}
