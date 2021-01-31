package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

import static com.mongodb.client.model.Indexes.compoundIndex;
import static org.jobrunr.storage.StorageProviderUtils.Jobs;

public class M001_CreateJobCollection extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase) {
        if (collectionExists(jobrunrDatabase, Jobs.NAME))
            return; //why: to be compatible with existing installations not using Migrations yet

        if (createCollection(jobrunrDatabase, Jobs.NAME)) {
            MongoCollection<Document> jobCollection = jobrunrDatabase.getCollection(Jobs.NAME, Document.class);
            jobCollection.createIndex(compoundIndex(Indexes.ascending(Jobs.FIELD_STATE), Indexes.ascending(Jobs.FIELD_SCHEDULED_AT)));
            jobCollection.createIndex(compoundIndex(Indexes.ascending(Jobs.FIELD_STATE), Indexes.ascending(Jobs.FIELD_UPDATED_AT)));
            jobCollection.createIndex(compoundIndex(Indexes.ascending(Jobs.FIELD_STATE), Indexes.descending(Jobs.FIELD_UPDATED_AT)));
            jobCollection.createIndex(compoundIndex(Indexes.ascending(Jobs.FIELD_STATE), Indexes.ascending(Jobs.FIELD_CREATED_AT)));
            jobCollection.createIndex(compoundIndex(Indexes.ascending(Jobs.FIELD_STATE), Indexes.ascending(Jobs.FIELD_JOB_SIGNATURE)));
        }
    }
}
