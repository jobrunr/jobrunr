package org.jobrunr.spring.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mongodb.client.*;
import com.mongodb.client.result.InsertOneResult;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.ClusterClient;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.spring.autoconfigure.health.JobRunrHealthIndicator;
import org.jobrunr.spring.autoconfigure.storage.*;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.storage.nosql.redis.JedisRedisStorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobRunrAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JobRunrAutoConfiguration.class,
                    JobRunrElasticSearchStorageAutoConfiguration.class,
                    JobRunrJedisStorageAutoConfiguration.class,
                    JobRunrLettuceStorageAutoConfiguration.class,
                    JobRunrMongoDBStorageAutoConfiguration.class,
                    JobRunrSqlStorageAutoConfiguration.class
            ));

    @Test
    void gsonIsIgnoredIfLibraryIsNotPresent() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withClassLoader(new FilteredClassLoader(Gson.class))
                .run((context) -> assertThat(context).getBean("jobRunrJsonMapper").isInstanceOf(JacksonJsonMapper.class));
    }

    @Test
    void jacksonIsIgnoredIfLibraryIsNotPresent() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withClassLoader(new FilteredClassLoader(ObjectMapper.class))
                .run((context) -> assertThat(context).getBean("jobRunrJsonMapper").isInstanceOf(GsonJsonMapper.class));
    }

    @Test
    void jobSchedulerEnabledAutoConfiguration() {
        this.contextRunner.withPropertyValues("org.jobrunr.job-scheduler.enabled=true").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(JobScheduler.class);
            assertThat(context).hasSingleBean(JobRequestScheduler.class);
            assertThat(context).doesNotHaveBean(JobRunrDashboardWebServer.class);
            assertThat(context).doesNotHaveBean(BackgroundJobServer.class);
        });
    }

    @Test
    void jobSchedulerDisabledAutoConfiguration() {
        this.contextRunner.withPropertyValues("org.jobrunr.job-scheduler.enabled=false").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).doesNotHaveBean(JobScheduler.class);
            assertThat(context).doesNotHaveBean(JobRunrDashboardWebServer.class);
            assertThat(context).doesNotHaveBean(BackgroundJobServer.class);
        });
    }

    @Test
    void dashboardAutoConfiguration() {
        this.contextRunner.withPropertyValues("org.jobrunr.dashboard.enabled=true").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(JobRunrDashboardWebServer.class);
            assertThat(context).doesNotHaveBean(BackgroundJobServer.class);
        });
    }

    @Test
    void backgroundJobServerAutoConfiguration() {
        this.contextRunner.withPropertyValues("org.jobrunr.background-job-server.enabled=true").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(BackgroundJobServer.class);
            assertThat(context).doesNotHaveBean(JobRunrDashboardWebServer.class);
        });
    }

    @Test
    void inMemoryStorageProviderAutoConfiguration() {
        this.contextRunner.withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(InMemoryStorageProvider.class);
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    void sqlStorageProviderAutoConfiguration() {
        this.contextRunner.withPropertyValues("org.jobrunr.database.skip-create=true").withUserConfiguration(SqlDataSourceConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(DefaultSqlStorageProvider.class);
            assertThat(context.getBean("storageProvider")).extracting("jobMapper").isNotNull();
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    void mongoDBStorageProviderAutoConfiguration() {
        this.contextRunner.withUserConfiguration(MongoDBStorageProviderConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(MongoDBStorageProvider.class);
            assertThat(context.getBean("storageProvider")).extracting("jobDocumentMapper").isNotNull();
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    void elasticSearchStorageProviderAutoConfiguration() {
        this.contextRunner.withUserConfiguration(ElasticSearchStorageProviderConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(ElasticSearchStorageProvider.class);
            assertThat(context.getBean("storageProvider")).extracting("elasticSearchDocumentMapper").isNotNull();
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    void jedisStorageProviderAutoConfiguration() {
        this.contextRunner.withUserConfiguration(JedisStorageProviderConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(JedisRedisStorageProvider.class);
            assertThat(context.getBean("storageProvider")).extracting("jobMapper").isNotNull();
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    void lettuceStorageProviderAutoConfiguration() {
        this.contextRunner.withUserConfiguration(LettuceStorageProviderConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(LettuceRedisStorageProvider.class);
            assertThat(context.getBean("storageProvider")).extracting("jobMapper").isNotNull();
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    void jobRunrHealthIndicatorAutoConfiguration() {
        this.contextRunner.withPropertyValues("org.jobrunr.background-job-server.enabled=true").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(JobRunrHealthIndicator.class);
            assertThat(context.getBean(JobRunrHealthIndicator.class).health().getStatus()).isEqualTo(Status.UP);
        });
    }

    @Test
    void jobRunrHealthIndicatorAutoConfigurationHealthIndicatorDisabled() {
        this.contextRunner
                .withPropertyValues(
                        "org.jobrunr.background-job-server.enabled=true",
                        "management.health.jobrunr.enabled=false"
                )
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).doesNotHaveBean(JobRunrHealthIndicator.class);
                });
    }

    @Configuration
    static class InMemoryStorageProviderConfiguration {
        @Bean
        public StorageProvider storageProvider() {
            return new InMemoryStorageProvider();
        }
    }

    @Configuration
    static class SqlDataSourceConfiguration {
        @Bean
        public DataSource dataSource() throws SQLException {
            DataSource dataSourceMock = mock(DataSource.class);
            Connection connectionMock = mock(Connection.class);
            DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
            when(dataSourceMock.getConnection()).thenReturn(connectionMock);
            when(connectionMock.getMetaData()).thenReturn(databaseMetaData);
            when(databaseMetaData.getURL()).thenReturn("jdbc:sqlite:this is not important");

            mockTablePresent(connectionMock, "jobrunr_jobs", "jobrunr_recurring_jobs", "jobrunr_backgroundjobservers", "jobrunr_metadata");

            return dataSourceMock;
        }

        private void mockTablePresent(Connection connectionMock, String... tableNames) throws SQLException {
            Statement statementMock = mock(Statement.class);
            when(connectionMock.createStatement()).thenReturn(statementMock);
            for (String tableName : tableNames) {
                ResultSet resultSetMock = mock(ResultSet.class);
                when(statementMock.executeQuery("select count(*) from " + tableName)).thenReturn(resultSetMock);
                when(resultSetMock.next()).thenReturn(true);
                when(resultSetMock.getInt(1)).thenReturn(1);
            }
        }
    }

    @Configuration
    static class MongoDBStorageProviderConfiguration {

        @Bean
        public MongoClient mongoClient() {
            MongoClient mongoClientMock = mock(MongoClient.class);
            MongoCollection migrationCollectionMock = mock(MongoCollection.class);
            when(migrationCollectionMock.find(any(Bson.class))).thenReturn(mock(FindIterable.class));
            MongoDatabase mongoDatabaseMock = mock(MongoDatabase.class);
            when(mongoDatabaseMock.getCollection(StorageProviderUtils.Migrations.NAME)).thenReturn(migrationCollectionMock);
            when(mongoDatabaseMock.listCollectionNames()).thenReturn(mock(MongoIterable.class));
            when(mongoClientMock.getDatabase("jobrunr")).thenReturn(mongoDatabaseMock);
            when(mongoDatabaseMock.getCollection(any(), eq(Document.class))).thenReturn(mock(MongoCollection.class));
            when(migrationCollectionMock.insertOne(any())).thenReturn(mock(InsertOneResult.class));
            return mongoClientMock;
        }
    }

    @Configuration
    static class ElasticSearchStorageProviderConfiguration {

        @Bean
        public RestHighLevelClient restHighLevelClient() throws IOException {
            RestHighLevelClient restHighLevelClientMock = mock(RestHighLevelClient.class);
            IndicesClient indicesClientMock = mock(IndicesClient.class);
            ClusterClient clusterClientMock = mock(ClusterClient.class);
            when(restHighLevelClientMock.indices()).thenReturn(indicesClientMock);
            when(restHighLevelClientMock.cluster()).thenReturn(clusterClientMock);
            when(indicesClientMock.exists(any(GetIndexRequest.class), eq(RequestOptions.DEFAULT))).thenReturn(true);

            GetResponse getResponse = mock(GetResponse.class);
            when(getResponse.isExists()).thenReturn(true);
            when(restHighLevelClientMock.get(any(GetRequest.class), eq(RequestOptions.DEFAULT))).thenReturn(getResponse);
            return restHighLevelClientMock;
        }
    }

    @Configuration
    static class JedisStorageProviderConfiguration {

        @Bean
        public JedisPool jedisPool() {
            JedisPool jedisPool = mock(JedisPool.class);
            when(jedisPool.getResource()).thenReturn(mock(Jedis.class));
            return jedisPool;
        }
    }

    @Configuration
    static class LettuceStorageProviderConfiguration {

        @Bean
        public RedisClient redisClient() {
            RedisClient redisClient = mock(RedisClient.class);
            StatefulRedisConnection connection = mock(StatefulRedisConnection.class);
            when(connection.sync()).thenReturn(mock(RedisCommands.class));
            when(redisClient.connect()).thenReturn(connection);
            return redisClient;
        }
    }
}
