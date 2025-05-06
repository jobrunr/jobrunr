package org.jobrunr.spring.autoconfigure;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListCollectionNamesIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.conversions.Bson;
import org.jobrunr.storage.StorageProviderUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Mocks {

    public static DataSource dataSource() throws SQLException {
        DataSource dataSourceMock = mock(DataSource.class);
        Connection connectionMock = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(dataSourceMock.getConnection()).thenReturn(connectionMock);
        when(connectionMock.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getURL()).thenReturn("jdbc:sqlite:this is not important");

        ResultSet resultSetMock = mock(ResultSet.class);
        when(databaseMetaData.getTables(null, null, "%", null)).thenReturn(resultSetMock);
        when(resultSetMock.next()).thenReturn(true, true, true, true, false);
        when(resultSetMock.getString("TABLE_NAME")).thenReturn("jobrunr_jobs", "jobrunr_recurring_jobs", "jobrunr_backgroundjobservers", "jobrunr_metadata");

        return dataSourceMock;
    }

    public static MongoClient mongoClient() {
        MongoClient mongoClientMock = mock(MongoClient.class);
        when(mongoClientMock.getCodecRegistry()).thenReturn(CodecRegistries.fromRegistries(
                CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.JAVA_LEGACY)),
                MongoClientSettings.getDefaultCodecRegistry()));
        MongoDatabase mongoDatabaseMock = mock(MongoDatabase.class);
        when(mongoClientMock.getDatabase("jobrunr")).thenReturn(mongoDatabaseMock);
        when(mongoDatabaseMock.listCollectionNames()).thenReturn(mock(ListCollectionNamesIterable.class));

        MongoCollection migrationCollectionMock = mock(MongoCollection.class);
        when(migrationCollectionMock.find(any(Bson.class))).thenReturn(mock(FindIterable.class));
        when(migrationCollectionMock.insertOne(any())).thenReturn(mock(InsertOneResult.class));
        when(mongoDatabaseMock.getCollection(StorageProviderUtils.Migrations.NAME)).thenReturn(migrationCollectionMock);

        ListIndexesIterable listIndicesMock = mock(ListIndexesIterable.class);
        when(listIndicesMock.spliterator()).thenReturn(mock(Spliterator.class));

        MongoCollection recurringJobCollectionMock = mock(MongoCollection.class);
        when(recurringJobCollectionMock.listIndexes()).thenReturn(listIndicesMock);

        MongoCollection jobCollectionMock = mock(MongoCollection.class);
        when(jobCollectionMock.listIndexes()).thenReturn(listIndicesMock);

        when(mongoDatabaseMock.getCollection(StorageProviderUtils.RecurringJobs.NAME, Document.class)).thenReturn(jobCollectionMock);
        when(mongoDatabaseMock.getCollection(StorageProviderUtils.Jobs.NAME, Document.class)).thenReturn(jobCollectionMock);
        when(mongoDatabaseMock.getCollection(StorageProviderUtils.BackgroundJobServers.NAME, Document.class)).thenReturn(mock(MongoCollection.class));
        when(mongoDatabaseMock.getCollection(StorageProviderUtils.Metadata.NAME, Document.class)).thenReturn(mock(MongoCollection.class));
        return mongoClientMock;
    }

}
