package org.jobrunr.storage.sql.common;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.SqlStorageProvider;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class SqlStorageProviderFactory {

    private SqlStorageProviderFactory() {
    }

    public static StorageProvider using(DataSource dataSource) {
        if (dataSource.getClass().getName().contains("sqlite")) {
            return getSqliteStorageProvider(dataSource);
        } else if (dataSource.getClass().getName().contains("h2")) {
            return getH2StorageProvider(dataSource);
        } else if (dataSource.getClass().getName().contains("postgres")) {
            return getPostgresStorageProvider(dataSource);
        } else if (dataSource.getClass().getName().contains("oracle")) {
            return getOracleStorageProvider(dataSource);
        } else if (dataSource.getClass().getName().contains("mariadb")) {
            return getMariaDbStorageProvider(dataSource);
        } else if (dataSource.getClass().getName().contains("hikari")) {
            return getStorageProviderUsingJdbcUrl(dataSource);
        } else if (dataSource.getClass().getName().contains("c3p0")) {
            return getStorageProviderUsingJdbcUrl(dataSource);
        } else if (dataSource.getClass().getName().contains("dbcp2")) {
            return getStorageProviderUsingUrl(dataSource);
        } else if (dataSource.getClass().getName().contains("tomcat")) {
            return getStorageProviderUsingUrl(dataSource);
        }
        throw unsupportedDataSourceException(dataSource);
    }

    private static StorageProvider getSqliteStorageProvider(DataSource dataSource) {
        return getStorageProvider(SqlStorageProvider.class.getPackage().getName() + ".sqlite.SqLiteStorageProvider", dataSource);
    }

    private static StorageProvider getH2StorageProvider(DataSource dataSource) {
        return getStorageProvider(SqlStorageProvider.class.getPackage().getName() + ".h2.H2StorageProvider", dataSource);
    }

    private static StorageProvider getPostgresStorageProvider(DataSource dataSource) {
        return getStorageProvider(SqlStorageProvider.class.getPackage().getName() + ".postgres.PostgresStorageProvider", dataSource);
    }

    private static StorageProvider getOracleStorageProvider(DataSource dataSource) {
        return getStorageProvider(SqlStorageProvider.class.getPackage().getName() + ".oracle.OracleStorageProvider", dataSource);
    }

    private static StorageProvider getMariaDbStorageProvider(DataSource dataSource) {
        return getStorageProvider(SqlStorageProvider.class.getPackage().getName() + ".mariadb.MariaDbStorageProvider", dataSource);
    }

    private static StorageProvider getStorageProviderUsingJdbcUrl(DataSource dataSource) {
        try {
            Class clazz = dataSource.getClass();
            Method method = clazz.getMethod("getJdbcUrl");
            final String jdbcUrl = method.invoke(dataSource).toString();
            return getStorageProviderByJdbcUrl(jdbcUrl, dataSource);
        } catch (ReflectiveOperationException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static StorageProvider getStorageProviderUsingUrl(DataSource dataSource) {
        try {
            Class clazz = dataSource.getClass();
            Method method = clazz.getMethod("getUrl");
            final String jdbcUrl = method.invoke(dataSource).toString();
            return getStorageProviderByJdbcUrl(jdbcUrl, dataSource);
        } catch (ReflectiveOperationException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static StorageProvider getStorageProviderByJdbcUrl(String jdbcUrl, DataSource dataSource) {
        if (jdbcUrl.startsWith("jdbc:sqlite")) {
            return getSqliteStorageProvider(dataSource);
        } else if (jdbcUrl.startsWith("jdbc:h2")) {
            return getH2StorageProvider(dataSource);
        } else if (jdbcUrl.startsWith("jdbc:postgres")) {
            return getPostgresStorageProvider(dataSource);
        } else if (jdbcUrl.startsWith("jdbc:mysql") || jdbcUrl.startsWith("jdbc:mariadb")) {
            return getMariaDbStorageProvider(dataSource);
        } else if (jdbcUrl.startsWith("jdbc:oracle")) {
            return getOracleStorageProvider(dataSource);
        }
        throw unsupportedDataSourceException(dataSource);
    }

    private static StorageProvider getStorageProvider(String className, DataSource dataSource) {
        try {
            final Class<?> jobStorageProvider = Class.forName(className);
            final Constructor<?> declaredConstructor = jobStorageProvider.getDeclaredConstructor(DataSource.class);
            return (StorageProvider) declaredConstructor.newInstance(dataSource);
        } catch (ReflectiveOperationException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static JobRunrException unsupportedDataSourceException(DataSource dataSource) {
        return new JobRunrException("Are you running an unsupported DataSource or Database? Please check the documentation. If you think this is wrong, please open an issue using the following url: https://github.com/jobrunr/jobrunr/issues/new?template=bug_report.md&title=%5BBUG%5D5%20-%20missing%20DataSource%20" + dataSource.getClass().getName());
    }
}
