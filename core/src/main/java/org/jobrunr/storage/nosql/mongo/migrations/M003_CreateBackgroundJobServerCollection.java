package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoDatabase;

import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class M003_CreateBackgroundJobServerCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase, String collectionPrefix) {
        String collectionName = elementPrefixer(collectionPrefix, BackgroundJobServers.NAME);
        if (collectionExists(jobrunrDatabase, collectionName))
            return; //why: to be compatible with existing installations not using Migrations yet

        createCollection(jobrunrDatabase, collectionName);
    }
}
