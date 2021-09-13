package org.jobrunr.quarkus.autoconfigure.storage;

import io.quarkus.arc.DefaultBean;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.quarkus.autoconfigure.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.util.Optional;

public class JobRunrSqlStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(@Any Instance<DataSource> dataSources, JobMapper jobMapper, JobRunrConfiguration configuration) {
        String tablePrefix = configuration.database.tablePrefix.orElse(null);
        DatabaseOptions databaseOptions = configuration.database.skipCreate ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource(dataSources, configuration), tablePrefix, databaseOptions);
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

    private DataSource getDataSource(Instance<DataSource> dataSources, JobRunrConfiguration configuration) {
        return dataSources.select(DataSource.class, toAnnotationQualifier(configuration.database.datasource)).get();
    }

    private static Annotation toAnnotationQualifier(Optional<String> dataSourceName) {
        return dataSourceName.map(datasource -> namedInstance(datasource)).orElse(defaultInstance());
    }

    private static Annotation defaultInstance() {
        return new Default() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Default.class;
            }
        };
    }

    private static Annotation namedInstance(String name) {
        return new Named() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Named.class;
            }

            @Override
            public String value() {
                return name;
            }

            @Override
            public String toString() {
                return "@javax.inject.Named(\"" + name + "\")";
            }
        };
    }
}
