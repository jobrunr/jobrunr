package org.jobrunr.spring.autoconfigure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import io.lettuce.core.RedisClient;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.spring.autoconfigure.health.JobRunrHealthIndicator;
import org.jobrunr.spring.autoconfigure.storage.JobRunrElasticSearchStorageAutoConfiguration;
import org.jobrunr.spring.autoconfigure.storage.JobRunrJedisStorageAutoConfiguration;
import org.jobrunr.spring.autoconfigure.storage.JobRunrLettuceStorageAutoConfiguration;
import org.jobrunr.spring.autoconfigure.storage.JobRunrMongoDBStorageAutoConfiguration;
import org.jobrunr.spring.autoconfigure.storage.JobRunrSqlStorageAutoConfiguration;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
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
import redis.clients.jedis.JedisPool;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThat;

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
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageDefaultTrue() {
        this.contextRunner
                .withPropertyValues("org.jobrunr.dashboard.enabled=true")
                .withPropertyValues("org.jobrunr.miscellaneous.allow-anonymous-data-usage=true")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(JobRunrDashboardWebServer.class);
                    assertThat(context.getBean(JobRunrDashboardWebServer.class))
                            .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", true);
                });
    }

    @Test
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageFalse() {
        this.contextRunner
                .withPropertyValues("org.jobrunr.dashboard.enabled=true")
                .withPropertyValues("org.jobrunr.miscellaneous.allow-anonymous-data-usage=false")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(JobRunrDashboardWebServer.class);
                    assertThat(context.getBean(JobRunrDashboardWebServer.class))
                            .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", false);
                });
    }

    @Test
    void backgroundJobServerAutoConfiguration() {
        this.contextRunner
                .withPropertyValues("org.jobrunr.background-job-server.enabled=true")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(BackgroundJobServer.class);
                    assertThat(context).doesNotHaveBean(JobRunrDashboardWebServer.class);
                });
    }

    @Test
    void backgroundJobServerAutoConfigurationTakesIntoAccountName() {
        this.contextRunner
                .withPropertyValues("org.jobrunr.background-job-server.enabled=true")
                .withPropertyValues("org.jobrunr.background-job-server.name=test")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(BackgroundJobServer.class);
                    assertThat(context.getBean(BackgroundJobServer.class))
                            .hasName("test");
                });
    }

    @Test
    void backgroundJobServerAutoConfigurationTakesIntoThreadTypeAndWorkerCount() {
        this.contextRunner
                .withPropertyValues("org.jobrunr.background-job-server.enabled=true")
                .withPropertyValues("org.jobrunr.background-job-server.worker-count=4")
                .withPropertyValues("org.jobrunr.background-job-server.thread-type=PlatformThreads")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(BackgroundJobServer.class);
                    assertThat(context.getBean(BackgroundJobServer.class));
                    assertThat(context.getBean(BackgroundJobServerConfiguration.class))
                            .hasWorkerCount(4);
                });
    }

    @Test
    void backgroundJobServerAutoConfigurationTakesIntoAccountDefaultNumberOfRetries() {
        this.contextRunner
                .withPropertyValues("org.jobrunr.background-job-server.enabled=true")
                .withPropertyValues("org.jobrunr.jobs.default-number-of-retries=3")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(BackgroundJobServer.class);
                    assertThat(context.getBean(BackgroundJobServer.class))
                            .hasRetryFilter(3);
                });
    }

    @Test
    void backgroundJobServerAutoConfigurationTakesIntoAccountAllJobsRequestSizes() {
        this.contextRunner
                .withPropertyValues("org.jobrunr.background-job-server.enabled=true")
                .withPropertyValues("org.jobrunr.background-job-server.scheduled-jobs-request-size=1")
                .withPropertyValues("org.jobrunr.background-job-server.orphaned-jobs-request-size=2")
                .withPropertyValues("org.jobrunr.background-job-server.succeeded-jobs-request-size=3")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context.getBean(BackgroundJobServerConfiguration.class))
                            .hasScheduledJobRequestSize(1)
                            .hasOrphanedJobRequestSize(2)
                            .hasSucceededJobRequestSize(3);
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
            assertThat(context.getBean("storageProvider")).extracting("documentMapper").isNotNull();
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
    void jobRunrDoesNotFailIfMultipleDatabasesAvailableAndValueConfigured() {
        this.contextRunner
                .withPropertyValues(
                        "org.jobrunr.database.skip-create=true",
                        "org.jobrunr.database.type=sql"
                )
                .withUserConfiguration(SqlDataSourceConfiguration.class, ElasticSearchStorageProviderConfiguration.class).run((context) -> {
                    assertThat(context).hasSingleBean(DefaultSqlStorageProvider.class);
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
            return Mocks.dataSource();
        }
    }

    @Configuration
    static class MongoDBStorageProviderConfiguration {

        @Bean
        public MongoClient mongoClient() {
            return Mocks.mongoClient();
        }
    }

    @Configuration
    static class ElasticSearchStorageProviderConfiguration {

        @Bean
        public ElasticsearchClient elasticsearchClient() throws IOException {
            return Mocks.elasticsearchClient();
        }
    }

    @Configuration
    static class JedisStorageProviderConfiguration {

        @Bean
        public JedisPool jedisPool() {
            return Mocks.jedisPool();
        }
    }

    @Configuration
    static class LettuceStorageProviderConfiguration {

        @Bean
        public RedisClient redisClient() {
            return Mocks.redisClient();
        }
    }
}
