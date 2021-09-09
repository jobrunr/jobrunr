package org.jobrunr.storage.nosql.documentdb;

import com.mongodb.client.MongoClient;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.utils.resilience.RateLimiter;

public class AmazonDocumentDBStorageProvider extends MongoDBStorageProvider {

    public AmazonDocumentDBStorageProvider(String hostName, int port) {
        super(hostName, port);
    }

    public AmazonDocumentDBStorageProvider(MongoClient mongoClient) {
        super(mongoClient);
    }

    public AmazonDocumentDBStorageProvider(MongoClient mongoClient, String dbName) {
        super(mongoClient, dbName);
    }

    public AmazonDocumentDBStorageProvider(MongoClient mongoClient, String dbName, DatabaseOptions databaseOptions) {
        super(mongoClient, dbName, databaseOptions);
    }

    public AmazonDocumentDBStorageProvider(MongoClient mongoClient, String dbName, String collectionPrefix) {
        super(mongoClient, dbName, collectionPrefix);
    }

    public AmazonDocumentDBStorageProvider(MongoClient mongoClient, String dbName, String collectionPrefix, DatabaseOptions databaseOptions) {
        super(mongoClient, dbName, collectionPrefix, databaseOptions);
    }

    public AmazonDocumentDBStorageProvider(MongoClient mongoClient, RateLimiter changeListenerNotificationRateLimit) {
        super(mongoClient, changeListenerNotificationRateLimit);
    }

    public AmazonDocumentDBStorageProvider(MongoClient mongoClient, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        super(mongoClient, databaseOptions, changeListenerNotificationRateLimit);
    }

    public AmazonDocumentDBStorageProvider(MongoClient mongoClient, String dbName, String collectionPrefix, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        super(mongoClient, dbName, collectionPrefix, databaseOptions, changeListenerNotificationRateLimit);
    }
}
