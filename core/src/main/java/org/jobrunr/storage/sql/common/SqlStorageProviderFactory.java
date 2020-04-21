package org.jobrunr.storage.sql.common;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.SqlStorageProvider;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;

public class SqlStorageProviderFactory {

    private SqlStorageProviderFactory() {
    }

    public static StorageProvider using(DataSource dataSource) {
        if (dataSource.getClass().getName().contains("sqlite")) {
            return getJobStorageProvider(SqlStorageProvider.class.getPackage().getName() + ".sqlite.SqLiteStorageProvider", dataSource);
        } else if (dataSource.getClass().getName().contains("h2")) {
            return getJobStorageProvider(SqlStorageProvider.class.getPackage().getName() + ".h2.H2StorageProvider", dataSource);
        } else if (dataSource.getClass().getName().contains("postgres")) {
            return getJobStorageProvider(SqlStorageProvider.class.getPackage().getName() + ".postgres.PostgresStorageProvider", dataSource);
        } else if (dataSource.getClass().getName().contains("oracle")) {
            return getJobStorageProvider(SqlStorageProvider.class.getPackage().getName() + ".oracle.OracleStorageProvider", dataSource);
        } else if (dataSource.getClass().getName().contains("mariadb")) {
            return getJobStorageProvider(SqlStorageProvider.class.getPackage().getName() + ".mariadb.MariaDbStorageProvider", dataSource);
        }
        throw new JobRunrException("Are you running an unsupported database? Please check the documentation");
    }

    private static StorageProvider getJobStorageProvider(String className, DataSource dataSource) {
        try {
            final Class<?> jobStorageProvider = Class.forName(className);
            final Constructor<?> declaredConstructor = jobStorageProvider.getDeclaredConstructor(DataSource.class);
            return (StorageProvider) declaredConstructor.newInstance(dataSource);
        } catch (ReflectiveOperationException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }
}
