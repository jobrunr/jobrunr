package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoDatabase;
import org.jobrunr.storage.StorageProviderUtils;

public class M002_CreateRecurringJobCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase) {
        if (collectionExists(jobrunrDatabase, StorageProviderUtils.RecurringJobs.NAME))
            return; //why: to be compatible with existing installations not using Migrations yet

        jobrunrDatabase.createCollection(StorageProviderUtils.RecurringJobs.NAME);
    }
}
