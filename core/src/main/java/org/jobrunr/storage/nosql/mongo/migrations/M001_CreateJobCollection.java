package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.jobrunr.storage.StorageProviderUtils;

import static com.mongodb.client.model.Indexes.compoundIndex;

public class M001_CreateJobCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase) {
        if (collectionExists(jobrunrDatabase, StorageProviderUtils.Jobs.NAME))
            return; //why: to be compatible with existing installations not using Migrations yet

        jobrunrDatabase.createCollection(StorageProviderUtils.Jobs.NAME);

        MongoCollection<Document> jobCollection = jobrunrDatabase.getCollection(StorageProviderUtils.Jobs.NAME, Document.class);
        jobCollection.createIndex(compoundIndex(Indexes.ascending(StorageProviderUtils.Jobs.FIELD_STATE), Indexes.ascending(StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT)));
        jobCollection.createIndex(compoundIndex(Indexes.ascending(StorageProviderUtils.Jobs.FIELD_STATE), Indexes.ascending(StorageProviderUtils.Jobs.FIELD_UPDATED_AT)));
        jobCollection.createIndex(compoundIndex(Indexes.ascending(StorageProviderUtils.Jobs.FIELD_STATE), Indexes.descending(StorageProviderUtils.Jobs.FIELD_UPDATED_AT)));
        jobCollection.createIndex(compoundIndex(Indexes.ascending(StorageProviderUtils.Jobs.FIELD_STATE), Indexes.ascending(StorageProviderUtils.Jobs.FIELD_CREATED_AT)));
        jobCollection.createIndex(compoundIndex(Indexes.ascending(StorageProviderUtils.Jobs.FIELD_STATE), Indexes.ascending(StorageProviderUtils.Jobs.FIELD_JOB_SIGNATURE)));
    }
}
