package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoDatabase;
import org.jobrunr.storage.nosql.mongo.MongoCollectionPrefixProcessor;

public class M004_CreateJobStatsCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase, MongoCollectionPrefixProcessor collectionPrefixProcessor) {
        //why: to be compatible with existing installations not using Migrations yet
    }
}
