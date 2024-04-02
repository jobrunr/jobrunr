package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import org.jobrunr.storage.StorageProviderUtils;

import static com.mongodb.client.model.Indexes.compoundIndex;

public class M008_UpdateJobsCollectionAddCarbonAwareDeadlineIndex extends MongoMigration {
    @Override
    public void runMigration(MongoDatabase jobrunrDatabase, String collectionPrefix) {
        String collectionName = StorageProviderUtils.elementPrefixer(collectionPrefix, StorageProviderUtils.Jobs.NAME);

        if (!collectionExists(jobrunrDatabase, collectionName)) {
            throw new IllegalStateException("The jobs collection does not exist. Cannot add an index for carbonAwareDeadline.");
        }

        // Create an ascending index on the carbonAwareDeadline field.
        jobrunrDatabase.getCollection(collectionName)
                .createIndex(compoundIndex(Indexes.ascending(StorageProviderUtils.Jobs.FIELD_CARBON_AWARE_DEADLINE)));

        System.out.println("Index for carbonAwareDeadline has been added to the " + collectionName + " collection.");
    }
}
