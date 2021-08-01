package org.jobrunr.quarkus.autoconfigure.storage;

import io.quarkus.arc.DefaultBean;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.quarkus.autoconfigure.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.sql.DataSource;

public class JobRunrSqlStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(DataSource dataSource, JobMapper jobMapper, JobRunrConfiguration configuration) {
        String tablePrefix = configuration.database.tablePrefix.orElse(null);
        DefaultSqlStorageProvider.DatabaseOptions databaseOptions = configuration.database.skipCreate ? DefaultSqlStorageProvider.DatabaseOptions.SKIP_CREATE : DefaultSqlStorageProvider.DatabaseOptions.CREATE;
        StorageProvider storageProvider = SqlStorageProviderFactory.using(dataSource, tablePrefix, databaseOptions);
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

}
