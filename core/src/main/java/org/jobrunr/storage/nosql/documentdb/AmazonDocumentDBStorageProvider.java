package org.jobrunr.storage.nosql.documentdb;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.utils.resilience.RateLimiter;

import static com.mongodb.ReadPreference.secondaryPreferred;
import static java.util.Collections.singletonList;

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

    public static AmazonDocumentDBStorageProvider amazonDocumentDBStorageProviderWithDefaultSetting(String hostName, int port, MongoCredential credential) {
        return new AmazonDocumentDBStorageProvider(getDocumentDBDefaultSetting(new ServerAddress(hostName, port), credential));
    }

    public static AmazonDocumentDBStorageProvider amazonDocumentDBStorageProviderWithDefaultSetting(String hostName, int port, MongoCredential credential, String dbName) {
        return new AmazonDocumentDBStorageProvider(getDocumentDBDefaultSetting(new ServerAddress(hostName, port), credential), dbName);
    }

    private static MongoClient getDocumentDBDefaultSetting(ServerAddress serverAddress, MongoCredential credential) {
        return MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder -> builder.hosts(singletonList(serverAddress)))
                        .credential(credential)
                        .codecRegistry(CodecRegistries.fromRegistries(
                                CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
                                MongoClientSettings.getDefaultCodecRegistry()
                        ))
                        .retryWrites(false)
                        .readPreference(secondaryPreferred())
                        .build());
    }
}
