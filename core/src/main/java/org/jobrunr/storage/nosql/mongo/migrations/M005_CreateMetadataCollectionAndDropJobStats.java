package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.nosql.mongo.MongoCollectionPrefixProcessor;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Indexes.compoundIndex;
import static org.jobrunr.storage.StorageProviderUtils.JobStats;
import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider.toMongoId;

public class M005_CreateMetadataCollectionAndDropJobStats extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase, MongoCollectionPrefixProcessor collectionPrefixProcessor) {
        String processedCollectionName = collectionPrefixProcessor.applyCollectionPrefix(Metadata.NAME);
        if (createCollection(jobrunrDatabase, processedCollectionName)) {
            MongoCollection<Document> metadataCollection = createMetadataCollection(jobrunrDatabase, processedCollectionName);
            migrateExistingAllTimeSucceededFromJobStatsToMetadataAndDropJobStats(jobrunrDatabase, metadataCollection);
        }
    }

    private MongoCollection<Document> createMetadataCollection(MongoDatabase jobrunrDatabase, String processedMetadataCollectionName) {
        MongoCollection<Document> metadataCollection = jobrunrDatabase.getCollection(processedMetadataCollectionName, Document.class);
        metadataCollection.createIndex(compoundIndex(Indexes.ascending(Metadata.FIELD_NAME), Indexes.ascending(Metadata.FIELD_OWNER)));
        metadataCollection.insertOne(initialAllTimeSucceededJobCounterDocument());
        return metadataCollection;
    }

    private void migrateExistingAllTimeSucceededFromJobStatsToMetadataAndDropJobStats(MongoDatabase jobrunrDatabase, MongoCollection<Document> metadataCollection) {
        if (!collectionExists(jobrunrDatabase, JobStats.NAME)) return;

        MongoCollection<Document> jobStatsCollection = jobrunrDatabase.getCollection(JobStats.NAME, Document.class);
        final Document jobStats = jobStatsCollection.find(eq(toMongoId(JobStats.FIELD_ID), JobStats.FIELD_STATS)).first();
        long existingAmount = jobStats != null ? jobStats.getInteger(StateName.SUCCEEDED.name()) : 0;
        metadataCollection.updateOne(eq(toMongoId(Metadata.FIELD_ID), Metadata.STATS_ID), Updates.inc(Metadata.FIELD_VALUE, existingAmount), new UpdateOptions().upsert(true));
        jobStatsCollection.drop();
    }

    private Document initialAllTimeSucceededJobCounterDocument() {
        final Document document = new Document();
        document.put(Metadata.FIELD_ID, Metadata.STATS_ID);
        document.put(Metadata.FIELD_NAME, "succeeded-jobs-counter");
        document.put(Metadata.FIELD_OWNER, "cluster");
        document.put(Metadata.FIELD_VALUE, 0L);
        return document;
    }
}
