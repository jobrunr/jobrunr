package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;

import java.util.stream.StreamSupport;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class M006_UpdateRecurringJobsCollectionAddCreatedAtIndex extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase, String collectionPrefix) {
        String collectionName = elementPrefixer(collectionPrefix, RecurringJobs.NAME);

        MongoCollection<Document> recurringJobCollection = jobrunrDatabase.getCollection(collectionName, Document.class);

        if (StreamSupport.stream(recurringJobCollection.listIndexes().spliterator(), false).noneMatch(doc -> doc.getString("name").contains(RecurringJobs.FIELD_CREATED_AT))) {
            recurringJobCollection.createIndex(Indexes.ascending(RecurringJobs.FIELD_CREATED_AT));
        }
    }
}
