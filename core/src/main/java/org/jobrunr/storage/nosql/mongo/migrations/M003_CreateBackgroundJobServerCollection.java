package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoDatabase;
import org.jobrunr.storage.StorageProviderUtils;

public class M003_CreateBackgroundJobServerCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase) {
        if (collectionExists(jobrunrDatabase, StorageProviderUtils.BackgroundJobServers.NAME))
            return; //why: to be compatible with existing installations not using Migrations yet

        jobrunrDatabase.createCollection(StorageProviderUtils.BackgroundJobServers.NAME);
    }
}
