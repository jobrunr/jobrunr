package org.jobrunr.autoconfigure.storage;

import org.jobrunr.autoconfigure.JobRunrProperties;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider.DatabaseOptions;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnBean(DataSource.class)
@ConditionalOnSingleCandidate(DataSource.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class JobRunrSqlStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider sqlStorageProvider(DataSource dataSource, JobMapper jobMapper, JobRunrProperties properties) {
        String tablePrefix = properties.getDatabase().getTablePrefix();
        DatabaseOptions databaseOptions = properties.getDatabase().isSkipCreate() ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        StorageProvider storageProvider = SqlStorageProviderFactory.using(dataSource, tablePrefix, databaseOptions);
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }
}
