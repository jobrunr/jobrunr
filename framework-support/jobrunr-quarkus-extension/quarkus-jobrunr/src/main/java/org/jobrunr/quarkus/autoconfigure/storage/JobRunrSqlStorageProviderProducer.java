package org.jobrunr.quarkus.autoconfigure.storage;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.util.Optional;

public class JobRunrSqlStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(@Any Instance<DataSource> dataSources, JobMapper jobMapper, JobRunrRuntimeConfiguration configuration) {
        if (configuration.database().type().isPresent() && !configuration.database().type().get().equalsIgnoreCase("sql")) return null;

        String tablePrefix = configuration.database().tablePrefix().orElse(null);
        DatabaseOptions databaseOptions = configuration.database().skipCreate() ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource(dataSources, configuration), tablePrefix, databaseOptions);
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

    private DataSource getDataSource(Instance<DataSource> dataSources, JobRunrRuntimeConfiguration configuration) {
        return dataSources.select(DataSource.class, toAnnotationQualifier(configuration.database().datasource())).get();
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

            @Override
            public int hashCode() {
                return 42;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Default;
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

            @Override
            public int hashCode() {
                return value().hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof Named named)) return false;
                return value().equals(named.value());
            }
        };
    }
}
