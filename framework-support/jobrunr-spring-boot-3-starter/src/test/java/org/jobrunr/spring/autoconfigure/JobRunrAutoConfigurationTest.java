package org.jobrunr.spring.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.server.JobActivatorShutdownException;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.server.threadpool.PlatformThreadPoolJobRunrExecutor;
import org.jobrunr.spring.autoconfigure.health.JobRunrHealthIndicator;
import org.jobrunr.spring.autoconfigure.storage.JobRunrMongoDBStorageAutoConfiguration;
import org.jobrunr.spring.autoconfigure.storage.JobRunrSqlStorageAutoConfiguration;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.stubs.Mocks;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.mockito.Mockito.doThrow;

public class JobRunrAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JobRunrAutoConfiguration.class,
                    JobRunrMongoDBStorageAutoConfiguration.class,
                    JobRunrSqlStorageAutoConfiguration.class
            ));

    @Test
    void onSpringShutdownInitiatedJobActivatorThrowsJobActivatorShutdownException() {
        ApplicationContext applicationContextMock = Mockito.mock(ApplicationContext.class);
        JobActivator jobActivator = new JobRunrAutoConfiguration().jobActivator(applicationContextMock);

        doThrow(new BeanCreationNotAllowedException("TestService", "Boem")).when(applicationContextMock).getBean(TestService.class);
        assertThatCode(() -> jobActivator.activateJob(TestService.class)).isInstanceOf(JobActivatorShutdownException.class);
    }

    @Test
    void selectJacksonJsonMapperIfNoOtherJsonSerializersPresent() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withClassLoader(new FilteredClassLoader(Gson.class, kotlinx.serialization.json.Json.class))
                .run((context) -> assertThat(context).getBean("jobRunrJsonMapper").isInstanceOf(JacksonJsonMapper.class));
    }

    @Test
    void selectGsonMapperIfNoOtherJsonSerializersPresent() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withClassLoader(new FilteredClassLoader(ObjectMapper.class, kotlinx.serialization.json.Json.class))
                .run((context) -> assertThat(context).getBean("jobRunrJsonMapper").isInstanceOf(GsonJsonMapper.class));
    }

    @Test
    void selectKotlinxSerializationIfNoOtherJsonSerializersPresent() {
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .withClassLoader(new FilteredClassLoader(ObjectMapper.class, Gson.class))
                .run((context) -> assertThat(context).getBean("jobRunrJsonMapper").isInstanceOf(KotlinxSerializationJsonMapper.class));
    }

    @Test
    void selectKotlinxJsonMapperIfJacksonAndKotlinxArePresent() {
        // If multiple serializers are in the classpath, the kotlinx serializer needs to get precedence
        // See https://github.com/spring-projects/spring-boot/issues/39853
        // later on we should support spring.mvc.converters.preferred-json-mapper
        this.contextRunner
                .withUserConfiguration(InMemoryStorageProvider.class)
                .run((context) -> assertThat(context).getBean("jobRunrJsonMapper").isInstanceOf(KotlinxSerializationJsonMapper.class));
    }

    @Test
    void jobSchedulerEnabledAutoConfiguration() {
        this.contextRunner.withPropertyValues("jobrunr.job-scheduler.enabled=true").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(JobScheduler.class);
            assertThat(context).hasSingleBean(JobRequestScheduler.class);
            assertThat(context).doesNotHaveBean(JobRunrDashboardWebServer.class);
            assertThat(context).doesNotHaveBean(BackgroundJobServer.class);
        });
    }

    @Test
    void jobSchedulerDisabledAutoConfiguration() {
        this.contextRunner.withPropertyValues("jobrunr.job-scheduler.enabled=false").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).doesNotHaveBean(JobScheduler.class);
            assertThat(context).doesNotHaveBean(JobRunrDashboardWebServer.class);
            assertThat(context).doesNotHaveBean(BackgroundJobServer.class);
        });
    }

    @Test
    void dashboardAutoConfiguration() {
        this.contextRunner.withPropertyValues("jobrunr.dashboard.enabled=true").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(JobRunrDashboardWebServer.class);
            assertThat(context).doesNotHaveBean(BackgroundJobServer.class);
        });
    }


    @Test
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageDefaultTrue() {
        this.contextRunner
                .withPropertyValues("jobrunr.dashboard.enabled=true")
                .withPropertyValues("jobrunr.miscellaneous.allow-anonymous-data-usage=true")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(JobRunrDashboardWebServer.class);
                    assertThat(context.getBean(JobRunrDashboardWebServer.class))
                            .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", true);
                });
    }

    @Test
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageFalse() {
        this.contextRunner
                .withPropertyValues("jobrunr.dashboard.enabled=true")
                .withPropertyValues("jobrunr.miscellaneous.allow-anonymous-data-usage=false")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(JobRunrDashboardWebServer.class);
                    assertThat(context.getBean(JobRunrDashboardWebServer.class))
                            .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", false);
                });
    }

    @Test
    void carbonAwareManagerAutoConfigurationIsDisabledByDefault() {
        this.contextRunner
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    BackgroundJobServer backgroundJobServer = context.getBean(BackgroundJobServer.class);
                    CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfiguration = backgroundJobServer.getConfiguration().getCarbonAwareJobProcessingConfiguration();
                    assertThat(carbonAwareJobProcessingConfiguration).hasEnabled(false);
                });
    }

    @Test
    void carbonAwareManagerAutoConfiguration() {
        this.contextRunner
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withPropertyValues("jobrunr.background-job-server.carbon-aware-job-processing.enabled=true")
                .withPropertyValues("jobrunr.background-job-server.carbon-aware-job-processing.area-code=FR")
                .withPropertyValues("jobrunr.background-job-server.carbon-aware-job-processing.api-client-connect-timeout=500ms")
                .withPropertyValues("jobrunr.background-job-server.carbon-aware-job-processing.api-client-read-timeout=300ms")
                .withPropertyValues("jobrunr.background-job-server.carbon-aware-job-processing.poll-interval-in-minutes=15")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    BackgroundJobServer backgroundJobServer = context.getBean(BackgroundJobServer.class);
                    CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfiguration = backgroundJobServer.getConfiguration().getCarbonAwareJobProcessingConfiguration();
                    assertThat(carbonAwareJobProcessingConfiguration)
                            .hasEnabled(true)
                            .hasApiClientConnectTimeout(Duration.ofMillis(500))
                            .hasApiClientReadTimeout(Duration.ofMillis(300))
                            .hasPollIntervalInMinutes(15)
                            .hasAreaCode("FR");
                });
    }

    @Test
    void backgroundJobServerAutoConfiguration() {
        this.contextRunner
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(BackgroundJobServer.class);
                    assertThat(context).doesNotHaveBean(JobRunrDashboardWebServer.class);
                });
    }

    @Test
    void backgroundJobServerAutoConfigurationTakesIntoAccountName() {
        this.contextRunner
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withPropertyValues("jobrunr.background-job-server.name=test")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(BackgroundJobServer.class);
                    assertThat(context.getBean(BackgroundJobServer.class))
                            .hasName("test");
                });
    }


    @Test
    void backgroundJobServerAutoConfigurationTakesIntoThreadTypeAndWorkerCount() {
        this.contextRunner
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withPropertyValues("jobrunr.background-job-server.worker-count=4")
                .withPropertyValues("jobrunr.background-job-server.thread-type=PlatformThreads")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(BackgroundJobServer.class);
                    assertThat(context.getBean(BackgroundJobServerConfiguration.class))
                            .hasWorkerCount(4);
                });
    }

    @Test
    void backgroundJobServerAutoConfigurationTakesAllBackgroundServerPollIntervals() {
        this.contextRunner
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withPropertyValues("jobrunr.background-job-server.poll-interval-in-seconds=5")
                .withPropertyValues("jobrunr.background-job-server.server-timeout-poll-interval-multiplicand=10")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(BackgroundJobServer.class);
                    assertThat(context.getBean(BackgroundJobServerConfiguration.class))
                            .hasPollIntervalInSeconds(5)
                            .hasServerTimeoutPollIntervalMultiplicand(10);
                });
    }

    @Test
    void backgroundJobServerAutoConfigurationTakesIntoAccountDefaultNumberOfRetries() {
        this.contextRunner
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withPropertyValues("jobrunr.jobs.default-number-of-retries=3")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
                    assertThat(context).hasSingleBean(BackgroundJobServer.class);
                    assertThat(context.getBean(BackgroundJobServer.class))
                            .hasRetryFilter(3);
                });
    }

    @Test
    void backgroundJobServerAutoConfigurationTakesIntoAccountAllJobsRequestSizes() {
        this.contextRunner
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withPropertyValues("jobrunr.background-job-server.scheduled-jobs-request-size=1")
                .withPropertyValues("jobrunr.background-job-server.orphaned-jobs-request-size=2")
                .withPropertyValues("jobrunr.background-job-server.succeeded-jobs-request-size=3")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context)
                        -> assertThat(context.getBean(BackgroundJobServerConfiguration.class))
                        .hasScheduledJobRequestSize(1)
                        .hasOrphanedJobRequestSize(2)
                        .hasSucceededJobRequestSize(3));
    }

    @Test
    void backgroundJobServerAutoConfigurationTakesIntoAccountInterruptJobsAwaitDurationOnStopBackgroundJobServer() {
        this.contextRunner
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withPropertyValues("jobrunr.background-job-server.interrupt_jobs_await_duration_on_stop=20")
                .withUserConfiguration(InMemoryStorageProvider.class).run((context)
                        -> assertThat(context.getBean(BackgroundJobServerConfiguration.class))
                        .hasInterruptJobsAwaitDurationOnStopBackgroundJobServer(Duration.ofSeconds(20)));
    }

    @Test
    void backgroundJobServerAutoConfigurationTakesIntoAccountCustomBackgroundJobServerWorkerPolicy() {
        this.contextRunner
                .withPropertyValues("jobrunr.background-job-server.enabled=true")
                .withUserConfiguration(BackgroundJobServerConfigurationWithCustomWorkerPolicy.class, InMemoryStorageProvider.class).run((context)
                        -> assertThat(context.getBean(BackgroundJobServerConfiguration.class))
                        .hasWorkerPolicyOfType(BackgroundJobServerConfigurationWithCustomWorkerPolicy.MyBackgroundJobServerWorkerPolicy.class));
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
        this.contextRunner.withPropertyValues("jobrunr.database.skip-create=true").withUserConfiguration(SqlDataSourceConfiguration.class).run((context) -> {
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
    void jobRunrDoesNotFailIfMultipleDatabasesAvailableAndValueConfigured() {
        this.contextRunner
                .withPropertyValues(
                        "jobrunr.database.skip-create=true",
                        "jobrunr.database.type=sql"
                )
                .withUserConfiguration(SqlDataSourceConfiguration.class, MongoDBStorageProviderConfiguration.class).run((context) -> {
                    assertThat(context).hasSingleBean(DefaultSqlStorageProvider.class);
                    assertThat(context.getBean("storageProvider")).extracting("jobMapper").isNotNull();
                    assertThat(context).hasSingleBean(JobScheduler.class);
                });
    }

    @Test
    void jobRunrHealthIndicatorAutoConfiguration() {
        this.contextRunner.withPropertyValues("jobrunr.background-job-server.enabled=true").withUserConfiguration(InMemoryStorageProvider.class).run((context) -> {
            assertThat(context).hasSingleBean(JobRunrHealthIndicator.class);
            assertThat(context.getBean(JobRunrHealthIndicator.class).health().getStatus()).isEqualTo(Status.UP);
        });
    }

    @Test
    void jobRunrHealthIndicatorAutoConfigurationHealthIndicatorDisabled() {
        this.contextRunner
                .withPropertyValues(
                        "jobrunr.background-job-server.enabled=true",
                        "management.health.jobrunr.enabled=false"
                )
                .withUserConfiguration(InMemoryStorageProvider.class).run((context)
                        -> assertThat(context).doesNotHaveBean(JobRunrHealthIndicator.class));
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
    static class BackgroundJobServerConfigurationWithCustomWorkerPolicy {

        @Bean
        public BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy() {
            return new MyBackgroundJobServerWorkerPolicy();
        }

        private static class MyBackgroundJobServerWorkerPolicy implements BackgroundJobServerWorkerPolicy {

            @Override
            public WorkDistributionStrategy toWorkDistributionStrategy(BackgroundJobServer backgroundJobServer) {
                return new BasicWorkDistributionStrategy(backgroundJobServer, 10);
            }

            @Override
            public JobRunrExecutor toJobRunrExecutor() {
                return new PlatformThreadPoolJobRunrExecutor(10, "my-prefix");
            }
        }
    }
}
