package org.jobrunr.autoconfigure.storage;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
@ConditionalOnBean(DataSource.class)
@ConditionalOnSingleCandidate(DataSource.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class JobRunrSqlStorageAutoConfiguration {

    @Bean(name = "storageProvider")
    @ConditionalOnMissingBean
    public StorageProvider sqlStorageProvider(DataSource dataSource, JobMapper jobMapper, Environment environment) {
        if (!environment.containsProperty("org.jobrunr.database.skip-create") && !Boolean.parseBoolean(environment.getProperty("org.jobrunr.database.skip-create"))) {
            StorageProvider storageProvider = SqlStorageProviderFactory.using(dataSource);
            storageProvider.setJobMapper(jobMapper);
            return storageProvider;
        } else {
            DefaultSqlStorageProvider storageProvider = new DefaultSqlStorageProvider(dataSource, DefaultSqlStorageProvider.DatabaseOptions.SKIP_CREATE);
            storageProvider.setJobMapper(jobMapper);
            return storageProvider;
        }
    }
}
