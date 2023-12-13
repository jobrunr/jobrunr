package org.jobrunr.storage.nosql.mongo.migrations;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jobrunr.utils.exceptions.Exceptions;

import java.util.ArrayList;

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
        return mongoDatabase.listCollectionNames().into(new ArrayList<>()).contains(collectionName);
    }

    protected void dropIndexes(MongoCollection<Document> mongoCollection) {
        Exceptions.retryOnException(() -> mongoCollection.dropIndexes(),
                this::isBackgroundOperationInProgressException, 5, 200L);
    }

    protected void createIndex(MongoCollection<Document> mongoCollection, Bson index, IndexOptions indexOptions) {
        Exceptions.retryOnException(() -> mongoCollection.createIndex(index, indexOptions),
                this::isBackgroundOperationInProgressException, 5, 200L);
    }

    protected Boolean isBackgroundOperationInProgressException(RuntimeException e) {
        return e instanceof MongoCommandException && ((MongoCommandException) e).getErrorCode() == 12587;
    }
}
