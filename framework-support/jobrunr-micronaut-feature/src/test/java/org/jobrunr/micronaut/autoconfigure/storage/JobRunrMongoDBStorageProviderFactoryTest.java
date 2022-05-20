package org.jobrunr.micronaut.autoconfigure.storage;

import com.mongodb.client.*;
import com.mongodb.client.result.InsertOneResult;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Spliterator;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(rebuildContext = true)
class JobRunrMongoDBStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @BeforeEach
    void setupMongoClient() {
        context.registerSingleton(mongoClient());
    }

    @Test
    void mongoDBStorageProviderAutoConfigurationTest() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(MongoDBStorageProvider.class)
                .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
    }

    public MongoClient mongoClient() {
        MongoClient mongoClientMock = mock(MongoClient.class);
        MongoDatabase mongoDatabaseMock = mock(MongoDatabase.class);
        when(mongoClientMock.getDatabase("jobrunr")).thenReturn(mongoDatabaseMock);
        when(mongoDatabaseMock.listCollectionNames()).thenReturn(mock(MongoIterable.class));

        MongoCollection migrationCollectionMock = mock(MongoCollection.class);
        when(migrationCollectionMock.find(any(Bson.class))).thenReturn(mock(FindIterable.class));
        when(migrationCollectionMock.insertOne(any())).thenReturn(mock(InsertOneResult.class));
        when(mongoDatabaseMock.getCollection(StorageProviderUtils.Migrations.NAME)).thenReturn(migrationCollectionMock);

        MongoCollection recurringJobCollectionMock = mock(MongoCollection.class);
        ListIndexesIterable recurringJobListIndicesMock = mock(ListIndexesIterable.class);
        when(recurringJobListIndicesMock.spliterator()).thenReturn(mock(Spliterator.class));
        when(recurringJobCollectionMock.listIndexes()).thenReturn(recurringJobListIndicesMock);
        when(mongoDatabaseMock.getCollection(StorageProviderUtils.RecurringJobs.NAME, Document.class)).thenReturn(recurringJobCollectionMock);

        when(mongoDatabaseMock.getCollection(StorageProviderUtils.Jobs.NAME, Document.class)).thenReturn(mock(MongoCollection.class));
        when(mongoDatabaseMock.getCollection(StorageProviderUtils.Metadata.NAME, Document.class)).thenReturn(mock(MongoCollection.class));
        return mongoClientMock;
    }
}
