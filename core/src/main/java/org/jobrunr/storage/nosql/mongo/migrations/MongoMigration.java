package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoDatabase;

import java.util.LinkedList;

public abstract class MongoMigration {

    public abstract void runMigration(MongoDatabase mongoDatabase, String collectionPrefix);

    protected boolean createCollection(MongoDatabase mongoDatabase, String name) {
        try {
            mongoDatabase.createCollection(name);
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
