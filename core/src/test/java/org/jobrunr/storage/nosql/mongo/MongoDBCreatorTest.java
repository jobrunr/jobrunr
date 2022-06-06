package org.jobrunr.storage.nosql.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jobrunr.JobRunrException;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationByClass;
import org.jobrunr.storage.nosql.mongo.migrations.M001_CreateJobCollection;
import org.jobrunr.storage.nosql.mongo.migrations.M002_CreateRecurringJobCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class MongoDBCreatorTest {

    @Container
    private static final GenericContainer mongoContainer = new GenericContainer("mongo:3.4").withExposedPorts(27017);

    @BeforeEach
    void clearAllCollections() {
        final MongoDatabase database = mongoClient().getDatabase(MongoDBStorageProvider.DEFAULT_DB_NAME);
        final ArrayList<String> collectionNames = database.listCollectionNames().into(new ArrayList<>());
        collectionNames.forEach(collectionName -> database.getCollection(collectionName).drop());
    }

    @Test
    void testMigrationsHappyPath() {
        MongoDBCreator mongoDBCreator = new MongoDBCreator(mongoClient(), MongoDBStorageProvider.DEFAULT_DB_NAME);

        assertThat(mongoDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_CreateJobCollection.class))).isTrue();
        assertThat(mongoDBCreator.isNewMigration(new NoSqlMigrationByClass(M002_CreateRecurringJobCollection.class))).isTrue();

        assertThatCode(mongoDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(mongoDBCreator::runMigrations).doesNotThrowAnyException();

        assertThat(mongoDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_CreateJobCollection.class))).isFalse();
        assertThat(mongoDBCreator.isNewMigration(new NoSqlMigrationByClass(M002_CreateRecurringJobCollection.class))).isFalse();
    }

    @Test
    void testValidateCollectionsNoCollectionPrefix() {
        MongoDBCreator mongoDBCreator = new MongoDBCreator(mongoClient(), MongoDBStorageProvider.DEFAULT_DB_NAME);
        assertThatThrownBy(mongoDBCreator::validateCollections)
                .isInstanceOf(JobRunrException.class)
                .hasMessage("Not all required collections are available by JobRunr!");

        mongoDBCreator.runMigrations();

        assertThatCode(mongoDBCreator::validateCollections).doesNotThrowAnyException();
        assertThat(mongoClient().getDatabase(MongoDBStorageProvider.DEFAULT_DB_NAME).listCollectionNames().into(new ArrayList<>())).hasSize(5);
    }

    @Test
    void testValidateCollectionsWithCollectionPrefix() {
        MongoDBCreator mongoDBCreator = new MongoDBCreator(mongoClient(), MongoDBStorageProvider.DEFAULT_DB_NAME, "MYCOLLECTIONPREFIX_");
        assertThatThrownBy(mongoDBCreator::validateCollections)
                .isInstanceOf(JobRunrException.class)
                .hasMessage("Not all required collections are available by JobRunr!");

        mongoDBCreator.runMigrations();

        assertThatCode(mongoDBCreator::validateCollections).doesNotThrowAnyException();
        assertThat(mongoClient().getDatabase(MongoDBStorageProvider.DEFAULT_DB_NAME).listCollectionNames().into(new ArrayList<>())).hasSize(5);
    }

    @Test
    void testMigrationsConcurrent() {
        MongoDBCreator mongoDBCreator = new MongoDBCreator(mongoClient(), MongoDBStorageProvider.DEFAULT_DB_NAME) {
            @Override
            protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
                return true;
            }
        };

        assertThatCode(mongoDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(mongoDBCreator::runMigrations).doesNotThrowAnyException();
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