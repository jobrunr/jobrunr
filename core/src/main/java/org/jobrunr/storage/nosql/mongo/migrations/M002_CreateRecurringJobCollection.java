package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoDatabase;

import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs;

public class M002_CreateRecurringJobCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase) {
        if (collectionExists(jobrunrDatabase, RecurringJobs.NAME))
            return; //why: to be compatible with existing installations not using Migrations yet

        createCollection(jobrunrDatabase, RecurringJobs.NAME);
    }
}
