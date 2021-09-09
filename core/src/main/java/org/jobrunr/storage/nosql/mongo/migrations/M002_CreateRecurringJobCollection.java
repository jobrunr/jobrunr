package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoDatabase;

import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class M002_CreateRecurringJobCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase, String collectionPrefix) {
        String collectionName = elementPrefixer(collectionPrefix, RecurringJobs.NAME);
        if (collectionExists(jobrunrDatabase, collectionName))
            return; //why: to be compatible with existing installations not using Migrations yet

        createCollection(jobrunrDatabase, collectionName);
    }
}
