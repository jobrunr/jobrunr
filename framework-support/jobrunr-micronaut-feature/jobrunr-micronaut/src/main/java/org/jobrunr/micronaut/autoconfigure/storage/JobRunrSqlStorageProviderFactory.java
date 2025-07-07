package org.jobrunr.micronaut.autoconfigure.storage;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration;
import org.jobrunr.micronaut.autoconfigure.storage.sql.DelegatingDatasourceExtractor;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;

import javax.sql.DataSource;

@Factory
@Requires(beans = {DataSource.class})
@Requires(property = "jobrunr.database.type", value = "sql", defaultValue = "sql")
public class JobRunrSqlStorageProviderFactory {

    @Inject
    private JobRunrConfiguration configuration;

    @Singleton
    @Primary
    public StorageProvider sqlStorageProvider(BeanContext beanContext, JobMapper jobMapper) {
        DataSource dataSource = getDataSource(beanContext);
        String tablePrefix = configuration.getDatabase().getTablePrefix().orElse(null);
        StorageProviderUtils.DatabaseOptions databaseOptions = configuration.getDatabase().isSkipCreate() ? StorageProviderUtils.DatabaseOptions.SKIP_CREATE : StorageProviderUtils.DatabaseOptions.CREATE;
        StorageProvider storageProvider = org.jobrunr.storage.sql.common.SqlStorageProviderFactory.using(dataSource, tablePrefix, databaseOptions);
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

    private DataSource getDataSource(BeanContext beanContext) {
        DataSource dataSource = configuration.getDatabase().getDatasource()
                .map(datasourceName -> beanContext.getBean(DataSource.class, Qualifiers.byName(datasourceName)))
                .orElseGet(() -> beanContext.getBean(DataSource.class));
        Class superClass = dataSource.getClass();
        while (superClass != null) {
            if ("io.micronaut.data.connection.jdbc.advice.DelegatingDataSource".equals(superClass.getName())) {
                return DelegatingDatasourceExtractor.extract(dataSource);
            } else {
                superClass = superClass.getSuperclass();
            }
        }
        return dataSource;
    }
}
