package org.jobrunr.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mongodb.client.*;
import io.lettuce.core.RedisClient;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.jobrunr.autoconfigure.storage.*;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.storage.nosql.redis.JedisRedisStorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public void gsonIsIgnoredIfLibraryIsNotPresent() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withClassLoader(new FilteredClassLoader(Gson.class))
                .run((context) -> assertThat(context).getBean("jsonMapper").isInstanceOf(JacksonJsonMapper.class));
    }

    @Test
    public void jacksonIsIgnoredIfLibraryIsNotPresent() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withClassLoader(new FilteredClassLoader(ObjectMapper.class))
                .run((context) -> assertThat(context).getBean("jsonMapper").isInstanceOf(GsonJsonMapper.class));
    }

    @Test
    public void dashboardAutoConfiguration() {
        this.contextRunner.withPropertyValues("org.jobrunr.dashboard.enabled=true").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(JobRunrDashboardWebServer.class);
            assertThat(context).doesNotHaveBean(BackgroundJobServer.class);
        });
    }

    @Test
    public void backgroundJobServerAutoConfiguration() {
        this.contextRunner.withPropertyValues("org.jobrunr.background-job-server.enabled=true").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(BackgroundJobServer.class);
            assertThat(context).doesNotHaveBean(JobRunrDashboardWebServer.class);
        });
    }

    @Test
    public void inMemoryStorageProviderAutoConfiguration() {
        this.contextRunner.withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(InMemoryStorageProvider.class);
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    public void sqlStorageProviderAutoConfiguration() {
        this.contextRunner.withPropertyValues("org.jobrunr.database.skip-create=true").withUserConfiguration(SqlDataSourceConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(DefaultSqlStorageProvider.class);
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    public void mongoDBStorageProviderAutoConfiguration() {
        this.contextRunner.withUserConfiguration(MongoDBStorageProviderConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(MongoDBStorageProvider.class);
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    @Disabled("see https://github.com/elastic/elasticsearch/issues/40534")
    public void elasticSearchStorageProviderAutoConfiguration() {
        this.contextRunner.withUserConfiguration(ElasticSearchStorageProviderConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(ElasticSearchStorageProvider.class);
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    public void jedisStorageProviderAutoConfiguration() {
        this.contextRunner.withUserConfiguration(JedisStorageProviderConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(JedisRedisStorageProvider.class);
            assertThat(context).hasSingleBean(JobScheduler.class);
        });
    }

    @Test
    public void lettuceStorageProviderAutoConfiguration() {
        this.contextRunner.withUserConfiguration(LettuceStorageProviderConfiguration.class).run((context) -> {
            assertThat(context).hasSingleBean(LettuceRedisStorageProvider.class);
            assertThat(context).hasSingleBean(JobScheduler.class);
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

            mockTablePresent(connectionMock, "jobrunr_jobs", "jobrunr_recurring_jobs", "jobrunr_backgroundjobservers", "jobrunr_jobs_stats");

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
            MongoIterable<String> mongoIterable = mock(ListDatabasesIterable.class);
            MongoCursor<String> mongoCursorMock = mock(MongoCursor.class);
            when(mongoClientMock.listDatabaseNames()).thenReturn(mongoIterable);
            when(mongoIterable.iterator()).thenReturn(mongoCursorMock);
            when(mongoCursorMock.hasNext()).thenReturn(true).thenReturn(false);
            when(mongoCursorMock.next()).thenReturn("jobrunr");

            when(mongoClientMock.getDatabase("jobrunr")).thenReturn(mock(MongoDatabase.class));
            return mongoClientMock;
        }
    }

    @Configuration
    static class ElasticSearchStorageProviderConfiguration {

        @Bean
        public RestHighLevelClient restHighLevelClient() throws IOException {
            RestHighLevelClient restHighLevelClientMock = mock(RestHighLevelClient.class);
            IndicesClient indicesClientMock = mock(IndicesClient.class);
            when(restHighLevelClientMock.indices()).thenReturn(indicesClientMock);
            when(indicesClientMock.exists(any(GetIndexRequest.class), eq(RequestOptions.DEFAULT))).thenReturn(true);
            return restHighLevelClientMock;
        }
    }

    @Configuration
    static class JedisStorageProviderConfiguration {

        @Bean
        public JedisPool jedisPool() {
            return mock(JedisPool.class);
        }
    }

    @Configuration
    static class LettuceStorageProviderConfiguration {

        @Bean
        public RedisClient redisClient() {
            return mock(RedisClient.class);
        }
    }
}
