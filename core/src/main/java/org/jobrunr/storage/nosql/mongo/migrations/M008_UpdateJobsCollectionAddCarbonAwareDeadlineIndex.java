package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.jobrunr.storage.StorageProviderUtils;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.compoundIndex;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_DEADLINE;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_STATE;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class M008_UpdateJobsCollectionAddCarbonAwareDeadlineIndex extends MongoMigration {
    @Override
    public void runMigration(MongoDatabase jobrunrDatabase, String collectionPrefix) {
        String collectionName = elementPrefixer(collectionPrefix, StorageProviderUtils.Jobs.NAME);
        MongoCollection<Document> jobCollection = jobrunrDatabase.getCollection(collectionName, Document.class);

        // idx for awaiting jobs that need to be fetched by JobZooKeeper ProcessCarbonAwareAwaitingJobsTask
        createIndex(jobCollection,
                compoundIndex(ascending(FIELD_STATE), ascending(FIELD_DEADLINE)),
                new IndexOptions()
                        .name("carbonAwareDeadlinePartialIdx")
                        .partialFilterExpression(and(eq(FIELD_STATE, AWAITING.name()), exists(FIELD_DEADLINE)))
        );
    }
}
