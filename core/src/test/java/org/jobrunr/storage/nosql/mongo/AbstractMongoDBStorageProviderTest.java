package org.jobrunr.storage.nosql.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.Arrays;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

public abstract class AbstractMongoDBStorageProviderTest extends StorageProviderTest {

    private static MongoClient mongoClient;

    protected abstract GenericContainer getMongoContainer();

    @Override
    protected void cleanup() {
        MongoDatabase jobrunrDb = mongoClient().getDatabase(MongoDBStorageProvider.DEFAULT_DB_NAME);

        jobrunrDb
                .listCollectionNames()
                .into(new ArrayList<>()).stream()
                .filter(collectionName -> !collectionName.equals(StorageProviderUtils.Migrations.NAME))
                .forEach(collectionName -> jobrunrDb.getCollection(collectionName).deleteMany(new Document()));
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final MongoDBStorageProvider dbStorageProvider = new MongoDBStorageProvider(mongoClient(), rateLimit().withoutLimits());
        dbStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return dbStorageProvider;
    }

    @Override
    protected ThrowingStorageProvider makeThrowingStorageProvider(StorageProvider storageProvider) {
        return new ThrowingMongoDBStorageProvider(storageProvider);
    }

    @AfterAll
    public static void closeMongoClient() {
        mongoClient.close();
        mongoClient = null;
    }

    private MongoClient mongoClient() {
        GenericContainer mongoContainer = getMongoContainer();
        if (mongoClient == null) {
            CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                    CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
                    MongoClientSettings.getDefaultCodecRegistry()
            );
            mongoClient = MongoClients.create(
                    MongoClientSettings.builder()
                            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoContainer.getContainerIpAddress(), mongoContainer.getMappedPort(27017)))))
                            .codecRegistry(codecRegistry)
                            .build());

        }
        return mongoClient;
    }

    protected static class ThrowingMongoDBStorageProvider extends ThrowingStorageProvider {

        public ThrowingMongoDBStorageProvider(StorageProvider storageProvider) {
            super(storageProvider, "jobCollection");
        }

        @Override
        protected void makeStorageProviderThrowException(StorageProvider storageProvider) {
            MongoCollection mockedJobCollection = mock(MongoCollection.class);
            MongoException mongoException = mock(MongoException.class);
            when(mockedJobCollection.updateOne(any(), (Bson) any())).thenThrow(mongoException);
            when(mockedJobCollection.bulkWrite(any())).thenThrow(mongoException);
            setInternalState(storageProvider, "jobCollection", mockedJobCollection);
        }
    }
}
