package org.jobrunr.storage.nosql.documentdb;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class AmazonDocumentDBCreatorTest {

    @Container
    private static final GenericContainer mongoContainer = new GenericContainer("mongo:3.6").withExposedPorts(27017);

    @BeforeEach
    void clearAllCollections() {
        final MongoDatabase database = mongoClient().getDatabase(MongoDBStorageProvider.DEFAULT_DB_NAME);
        final ArrayList<String> collectionNames = database.listCollectionNames().into(new ArrayList<>());
        collectionNames.forEach(collectionName -> database.getCollection(collectionName).drop());
    }

    @Test
    void testMigrationsHappyPath() {
        AmazonDocumentDBCreator amazonDocumentDBCreator = new AmazonDocumentDBCreator(mongoClient(), MongoDBStorageProvider.DEFAULT_DB_NAME);

        assertThatCode(amazonDocumentDBCreator::runMigrations).doesNotThrowAnyException();
    }


    private MongoClient mongoClient() {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
                MongoClientSettings.getDefaultCodecRegistry()
        );
        return MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoContainer.getHost(), mongoContainer.getMappedPort(27017)))))
                        .codecRegistry(codecRegistry)
                        .build());
    }
}