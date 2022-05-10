package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.compoundIndex;
import static org.jobrunr.storage.StorageProviderUtils.Jobs;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_STATE;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class M006_AddIndices extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase, String collectionPrefix) {
        MongoCollection<Document> backgroundJobServerCollection = jobrunrDatabase.getCollection(elementPrefixer(collectionPrefix, BackgroundJobServers.NAME), Document.class);
        backgroundJobServerCollection.createIndex(ascending(BackgroundJobServers.FIELD_FIRST_HEARTBEAT));
        backgroundJobServerCollection.createIndex(ascending(BackgroundJobServers.FIELD_LAST_HEARTBEAT));

        MongoCollection<Document> jobCollection = jobrunrDatabase.getCollection(elementPrefixer(collectionPrefix, Jobs.NAME), Document.class);
        jobCollection.createIndex(compoundIndex(ascending(FIELD_STATE), ascending(Jobs.FIELD_RECURRING_JOB_ID), ascending(Jobs.FIELD_UPDATED_AT)));
    }
}
