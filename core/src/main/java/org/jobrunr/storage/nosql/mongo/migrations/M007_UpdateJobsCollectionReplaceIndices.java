package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProviderUtils.Jobs;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Indexes.*;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.*;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class M007_UpdateJobsCollectionReplaceIndices extends MongoMigration {

    @Override
    public void runMigration(MongoDatabase jobrunrDatabase, String collectionPrefix) {
        String collectionName = elementPrefixer(collectionPrefix, Jobs.NAME);

        MongoCollection<Document> jobCollection = jobrunrDatabase.getCollection(collectionName, Document.class);

        jobCollection.dropIndexes();

        // idx for recurring jobs that need to be fetched by JobZooKeeper ProcessRecurringJobsTask
        jobCollection.createIndex(
                compoundIndex(ascending(FIELD_STATE), ascending(FIELD_RECURRING_JOB_ID)),
                new IndexOptions().name("recurringJobPartialIdx").partialFilterExpression(exists(FIELD_RECURRING_JOB_ID)));

        // idx for scheduled jobs that need to be fetched by JobZooKeeper ProcessScheduledJobsTask
        jobCollection.createIndex(
                compoundIndex(ascending(FIELD_STATE), ascending(FIELD_SCHEDULED_AT)),
                new IndexOptions().name("scheduledPartialIdx").partialFilterExpression(eq(FIELD_STATE, StateName.SCHEDULED.name())));

        // idx for UI by state and DistinctJobSignatures
        jobCollection.createIndex(
                compoundIndex(ascending(FIELD_STATE), ascending(FIELD_UPDATED_AT)),
                new IndexOptions().name("jobsByStateUpdatedAtAscIdx"));
        jobCollection.createIndex(
                compoundIndex(ascending(FIELD_STATE), descending(FIELD_UPDATED_AT)),
                new IndexOptions().name("jobsByStateUpdatedAtDescIdx"));
    }
}
