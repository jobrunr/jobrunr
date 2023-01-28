package org.jobrunr.storage.nosql.documentdb;

import com.mongodb.client.MongoClient;
import org.jobrunr.storage.nosql.mongo.MongoDBCreator;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;

import static java.util.Arrays.asList;


public class AmazonDocumentDBCreator extends MongoDBCreator {


    public AmazonDocumentDBCreator(MongoClient mongoClient, String dbName) {
        this(mongoClient, dbName, null);
    }

    public AmazonDocumentDBCreator(MongoClient mongoClient, String dbName, String collectionPrefix) {
        super(asList(MongoDBStorageProvider.class, AmazonDocumentDBStorageProvider.class), mongoClient, dbName, collectionPrefix);
    }
}
