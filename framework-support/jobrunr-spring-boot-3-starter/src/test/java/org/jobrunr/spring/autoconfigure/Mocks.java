package org.jobrunr.spring.autoconfigure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.ElasticsearchClusterClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListCollectionNamesIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jobrunr.storage.StorageProviderUtils;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Mocks {

    public static ElasticsearchClient elasticsearchClient() throws IOException {
        final ElasticsearchClient client = mock(ElasticsearchClient.class);

        final GetResponse<?> getResponse = mock(GetResponse.class);
        when(getResponse.found()).thenReturn(true);
        when(client.get(any(Function.class), any())).thenAnswer(args -> getResponse);

        final BooleanResponse exists = new BooleanResponse(true);
        when(client.exists(any(Function.class))).thenReturn(exists);

        final ElasticsearchIndicesClient indices = mock(ElasticsearchIndicesClient.class);
        when(client.indices()).thenReturn(indices);

        when(indices.exists(any(Function.class))).thenReturn(exists);
        when(indices.exists(any(ExistsRequest.class))).thenReturn(exists);

        final ElasticsearchClusterClient cluster = mock(ElasticsearchClusterClient.class);
        when(client.cluster()).thenReturn(cluster);

        return client;
    }

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

    public static RedisClient redisClient() {
        RedisClient redisClient = mock(RedisClient.class);
        StatefulRedisConnection connection = mock(StatefulRedisConnection.class);
        when(connection.sync()).thenReturn(mock(RedisCommands.class));
        when(redisClient.connect()).thenReturn(connection);
        return redisClient;
    }

    public static LettuceConnectionFactory lettuceConnectionFactory() {
        RedisClient redisClient = redisClient();
        LettuceConnectionFactory lettuceConnectionFactory = mock(LettuceConnectionFactory.class);
        when(lettuceConnectionFactory.getNativeClient()).thenReturn(redisClient);
        return lettuceConnectionFactory;
    }

    public static JedisPool jedisPool() {
        JedisPool jedisPool = mock(JedisPool.class);
        when(jedisPool.getResource()).thenReturn(mock(Jedis.class));
        return jedisPool;
    }
}
