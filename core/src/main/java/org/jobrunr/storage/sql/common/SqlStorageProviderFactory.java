package org.jobrunr.storage.sql.common;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.SqlStorageProvider;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class SqlStorageProviderFactory {

    private SqlStorageProviderFactory() {
    }

    public static StorageProvider using(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            return getStorageProviderByJdbcUrl(jdbcUrl, dataSource);
        } catch (SQLException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static StorageProvider getStorageProviderByJdbcUrl(String jdbcUrl, DataSource dataSource) {
        final Class<SqlStorageProvider> storageProviderClassByJdbcUrl = getStorageProviderClassByJdbcUrl(jdbcUrl);
        return getStorageProvider(storageProviderClassByJdbcUrl, dataSource);
    }

    static Class<SqlStorageProvider> getStorageProviderClassByJdbcUrl(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:sqlite")) {
            return getStorageProviderClass(SqlStorageProvider.class.getPackage().getName() + ".sqlite.SqLiteStorageProvider");
        } else if (jdbcUrl.startsWith("jdbc:h2")) {
            return getStorageProviderClass(SqlStorageProvider.class.getPackage().getName() + ".h2.H2StorageProvider");
        } else if (jdbcUrl.startsWith("jdbc:postgres")) {
            return getStorageProviderClass(SqlStorageProvider.class.getPackage().getName() + ".postgres.PostgresStorageProvider");
        } else if (jdbcUrl.startsWith("jdbc:mysql") || jdbcUrl.startsWith("jdbc:mariadb")) {
            return getStorageProviderClass(SqlStorageProvider.class.getPackage().getName() + ".mariadb.MariaDbStorageProvider");
        } else if (jdbcUrl.startsWith("jdbc:oracle")) {
            return getStorageProviderClass(SqlStorageProvider.class.getPackage().getName() + ".oracle.OracleStorageProvider");
        } else if (jdbcUrl.startsWith("jdbc:sqlserver")) {
            return getStorageProviderClass(SqlStorageProvider.class.getPackage().getName() + ".sqlserver.SQLServerStorageProvider");
        }
        throw unsupportedDataSourceException(jdbcUrl);
    }

    private static StorageProvider getStorageProvider(Class<SqlStorageProvider> jobStorageProviderClass, DataSource dataSource) {
        try {
            final Constructor<?> declaredConstructor = jobStorageProviderClass.getDeclaredConstructor(DataSource.class);
            return (StorageProvider) declaredConstructor.newInstance(dataSource);
        } catch (ReflectiveOperationException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static Class<SqlStorageProvider> getStorageProviderClass(String className) {
        try {
            return cast(Class.forName(className));
        } catch (ReflectiveOperationException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static JobRunrException unsupportedDataSourceException(String jdbcUrl) {
        return new JobRunrException("Are you running an unsupported DataSource or Database? Please check the documentation. If you think this is wrong, please open an issue using the following url: https://github.com/jobrunr/jobrunr/issues/new?template=bug_report.md&title=%5BBUG%5D5%20-%20missing%20DataSource%20" + jdbcUrl);
    }
}
