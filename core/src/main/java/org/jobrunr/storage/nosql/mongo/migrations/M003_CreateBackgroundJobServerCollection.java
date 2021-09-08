package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoDatabase;
import org.jobrunr.storage.nosql.mongo.MongoCollectionPrefixProcessor;

import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;

public class M003_CreateBackgroundJobServerCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase, MongoCollectionPrefixProcessor collectionPrefixProcessor) {
        String processedCollectionName = collectionPrefixProcessor.applyCollectionPrefix(BackgroundJobServers.NAME);
        if (collectionExists(jobrunrDatabase, processedCollectionName))
            return; //why: to be compatible with existing installations not using Migrations yet

        createCollection(jobrunrDatabase, processedCollectionName);
    }
}
