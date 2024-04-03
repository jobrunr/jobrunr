package org.jobrunr.storage.nosql.documentdb.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.nosql.mongo.migrations.MongoMigration;

import static com.mongodb.client.model.Indexes.*;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class M008_UpdateJobsCollectionAddCarbonAwareDeadline extends MongoMigration {
    public void runMigration(MongoDatabase jobrunrDatabase, String collectionPrefix) {
        String collectionName = elementPrefixer(collectionPrefix, StorageProviderUtils.Jobs.NAME);

        MongoCollection<Document> jobCollection = jobrunrDatabase.getCollection(collectionName, Document.class);

        // idx for awaiting jobs that need to be fetched by JobZooKeeper ProcessAwaitingJobsTask
        createIndex(jobCollection,
                compoundIndex(ascending(StorageProviderUtils.Jobs.FIELD_CARBON_AWARE_DEADLINE)),
                new IndexOptions().name("carbonAwareDeadlineIdx"));
    }
}
