package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoDatabase;

import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;

public class M003_CreateBackgroundJobServerCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase) {
        if (collectionExists(jobrunrDatabase, BackgroundJobServers.NAME))
            return; //why: to be compatible with existing installations not using Migrations yet

        createCollection(jobrunrDatabase, BackgroundJobServers.NAME);
    }
}
