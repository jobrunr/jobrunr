package org.jobrunr.spring.autoconfigure.storage;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

@AutoConfiguration(after = DataSourceAutoConfiguration.class, before = JobRunrAutoConfiguration.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "jobrunr.database", name = "type", havingValue = "sql", matchIfMissing = true)
public class JobRunrSqlStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @DependsOnDatabaseInitialization
    @ConditionalOnMissingBean
    public StorageProvider sqlStorageProvider(BeanFactory beanFactory, JobMapper jobMapper, JobRunrProperties properties) {
        String tablePrefix = properties.getDatabase().getTablePrefix();
        DatabaseOptions databaseOptions = properties.getDatabase().isSkipCreate() ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource(beanFactory, properties), tablePrefix, databaseOptions);
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

    private DataSource getDataSource(BeanFactory beanFactory, JobRunrProperties properties) {
        if (isNotNullOrEmpty(properties.getDatabase().getDatasource())) {
            return beanFactory.getBean(properties.getDatabase().getDatasource(), DataSource.class);
        } else {
            return beanFactory.getBean(DataSource.class);
        }
    }
}
