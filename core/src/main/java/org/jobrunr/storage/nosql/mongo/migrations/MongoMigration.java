package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoDatabase;
import org.jobrunr.storage.StorageProviderUtils;

import java.util.LinkedList;

public abstract class MongoMigration {

    public abstract void runMigration(MongoDatabase mongoDatabase);

    protected boolean createCollection(MongoDatabase mongoDatabase, String name) {
        try {
            mongoDatabase.createCollection(StorageProviderUtils.Metadata.NAME);
            return true;
        } catch (MongoCommandException mongoCommandException) {
            if (mongoCommandException.getErrorCode() == 48) {
                return false;
            }
            throw mongoCommandException;
        }
    }

    protected boolean collectionExists(MongoDatabase mongoDatabase, String collectionName) {
        return mongoDatabase.listCollectionNames().into(new LinkedList<>()).contains(collectionName);
    }
}
