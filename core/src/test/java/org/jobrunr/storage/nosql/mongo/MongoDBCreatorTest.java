package org.jobrunr.storage.nosql.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationByClass;
import org.jobrunr.storage.nosql.mongo.migrations.M001_CreateJobCollection;
import org.jobrunr.storage.nosql.mongo.migrations.M002_CreateRecurringJobCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class MongoDBCreatorTest {

    @Container
    private static final GenericContainer mongoContainer = new GenericContainer("mongo:3.4").withExposedPorts(27017);

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
                        .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoContainer.getContainerIpAddress(), mongoContainer.getMappedPort(27017)))))
                        .codecRegistry(codecRegistry)
                        .build());
    }
}