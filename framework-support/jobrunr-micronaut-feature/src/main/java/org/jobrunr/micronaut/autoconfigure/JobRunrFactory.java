package org.jobrunr.micronaut.autoconfigure;

import com.mongodb.client.MongoClient;
import io.lettuce.core.RedisClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.elasticsearch.client.RestHighLevelClient;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

import javax.sql.DataSource;

import static java.util.Collections.emptyList;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.reflection.ReflectionUtils.newInstance;

@Factory
public class JobRunrFactory {

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private JobRunrConfiguration configuration;


    @Singleton
    @Requires(property = "jobrunr.job-scheduler.enabled", value = "true")
    public JobScheduler jobScheduler(StorageProvider storageProvider) {
        final JobDetailsGenerator jobDetailsGenerator = newInstance(configuration.getJobScheduler().getJobDetailsGenerator().orElse(CachingJobDetailsGenerator.class.getName()));
        return new JobScheduler(storageProvider, jobDetailsGenerator, emptyList());
    }

    @Singleton
    @Requires(property = "jobrunr.job-scheduler.enabled", value = "true")
    public JobRequestScheduler jobRequestScheduler(StorageProvider storageProvider) {
        return new JobRequestScheduler(storageProvider, emptyList());
    }

    @Singleton
    @Requires(property = "jobrunr.background-job-server.enabled", value = "true")
    public BackgroundJobServerConfiguration backgroundJobServerConfiguration() {
        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();
        configuration.getBackgroundJobServer().getPollIntervalInSeconds().ifPresent(backgroundJobServerConfiguration::andPollIntervalInSeconds);
        configuration.getBackgroundJobServer().getWorkerCount().ifPresent(backgroundJobServerConfiguration::andWorkerCount);
        configuration.getBackgroundJobServer().getDeleteSucceededJobsAfter().ifPresent(backgroundJobServerConfiguration::andDeleteSucceededJobsAfter);
        configuration.getBackgroundJobServer().getPermanentlyDeleteDeletedJobsAfter().ifPresent(backgroundJobServerConfiguration::andPermanentlyDeleteDeletedJobsAfter);
        return backgroundJobServerConfiguration;
    }

    @Singleton
    @Requires(property = "jobrunr.background-job-server.enabled", value = "true")
    public BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        return new BackgroundJobServer(storageProvider, jobRunrJsonMapper, jobActivator, backgroundJobServerConfiguration);
    }

    @Singleton
    @Requires(property = "jobrunr.dashboard.enabled", value = "true")
    public JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration() {
        final JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration = usingStandardDashboardConfiguration();
        configuration.getDashboard().getPort().ifPresent(dashboardWebServerConfiguration::andPort);
        if (configuration.getDashboard().getUsername().isPresent() && configuration.getDashboard().getPassword().isPresent()) {
            dashboardWebServerConfiguration.andBasicAuthentication(configuration.getDashboard().getUsername().get(), configuration.getDashboard().getPassword().get());
        }
        return dashboardWebServerConfiguration;
    }

    @Singleton
    @Requires(property = "jobrunr.dashboard.enabled", value = "true")
    public JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration) {
        return new JobRunrDashboardWebServer(storageProvider, jobRunrJsonMapper, dashboardWebServerConfiguration);
    }

    @Singleton
    public JobActivator jobActivator() {
        return new JobActivator() {
            @Override
            public <T> T activateJob(Class<T> aClass) {
                return applicationContext.getBean(aClass);
            }
        };
    }

    @Singleton
    public JobMapper jobMapper(JsonMapper jobRunrJsonMapper) {
        return new JobMapper(jobRunrJsonMapper);
    }

    @Singleton
    public JsonMapper jobRunrJsonMapper() {
        return new JacksonJsonMapper();
    }

    @Singleton
    public StorageProvider storageProvider(JobMapper jobMapper) {
        final InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

    @Singleton
    @Primary
    @Requires(beans = {DataSource.class})
    public StorageProvider sqlStorageProvider(BeanContext beanContext, JobMapper jobMapper) {
        DataSource dataSource = configuration.getDatabase().getDatasource()
                .map(datasourceName -> beanContext.getBean(DataSource.class, Qualifiers.byName(datasourceName)))
                .orElse(beanContext.getBean(DataSource.class));
        String tablePrefix = configuration.getDatabase().getTablePrefix().orElse(null);
        DefaultSqlStorageProvider.DatabaseOptions databaseOptions = configuration.getDatabase().isSkipCreate() ? DefaultSqlStorageProvider.DatabaseOptions.SKIP_CREATE : DefaultSqlStorageProvider.DatabaseOptions.CREATE;
        StorageProvider storageProvider = SqlStorageProviderFactory.using(dataSource, tablePrefix, databaseOptions);
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

    @Singleton
    @Primary
    @Requires(beans = {MongoClient.class})
    public StorageProvider mongoDBStorageProvider(MongoClient mongoClient, JobMapper jobMapper) {
        MongoDBStorageProvider mongoDBStorageProvider = new MongoDBStorageProvider(mongoClient);
        mongoDBStorageProvider.setJobMapper(jobMapper);
        return mongoDBStorageProvider;
    }

    @Singleton
    @Primary
    @Requires(beans = {RedisClient.class})
    public StorageProvider lettuceRedisStorageProvider(RedisClient redisClient, JobMapper jobMapper) {
        LettuceRedisStorageProvider lettuceRedisStorageProvider = new LettuceRedisStorageProvider(redisClient);
        lettuceRedisStorageProvider.setJobMapper(jobMapper);
        return lettuceRedisStorageProvider;
    }

    @Singleton
    @Primary
    @Requires(beans = {RestHighLevelClient.class})
    public StorageProvider elasticSearchStorageProvider(RestHighLevelClient restHighLevelClient, JobMapper jobMapper) {
        ElasticSearchStorageProvider elasticSearchStorageProvider = new ElasticSearchStorageProvider(restHighLevelClient);
        elasticSearchStorageProvider.setJobMapper(jobMapper);
        return elasticSearchStorageProvider;
    }
}
