package org.jobrunr.storage.sql.common;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.utils.reflection.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class SqlStorageProviderFactory {

    private SqlStorageProviderFactory() {
    }

    public static StorageProvider using(DataSource dataSource) {
        try {
            final Optional<String> jdbcUrlViaMethod = getUrlViaMethod(dataSource);
            if (jdbcUrlViaMethod.isPresent()) {
                return getStorageProviderByJdbcUrl(jdbcUrlViaMethod.get(), dataSource);
            }

            final Optional<String> jdbcUrlViaField = getUrlViaField(dataSource);
            if (jdbcUrlViaField.isPresent()) {
                return getStorageProviderByJdbcUrl(jdbcUrlViaField.get(), dataSource);
            }

            throw unsupportedDataSourceException(dataSource);
        } catch (ReflectiveOperationException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static Optional<String> getUrlViaMethod(DataSource dataSource) throws ReflectiveOperationException {
        final Optional<Method> urlMethod = Arrays.asList(dataSource.getClass().getMethods())
                .stream()
                .filter(m -> "getUrl".equals(m.getName()) || "getJdbcUrl".equals(m.getName()))
                .findFirst();

        if (urlMethod.isPresent()) {
            return Optional.of(urlMethod.get().invoke(dataSource).toString());
        }
        return Optional.empty();
    }

    private static Optional<String> getUrlViaField(DataSource dataSource) throws ReflectiveOperationException {
        final Optional<Field> urlField = Arrays.asList(dataSource.getClass().getDeclaredFields())
                .stream()
                .filter(f -> "url".equals(f.getName()))
                .findFirst();
        if (urlField.isPresent()) {
            final Field field = urlField.get();
            ReflectionUtils.makeAccessible(field);
            final Object urlAsObject = field.get(dataSource);
            if (urlAsObject != null) {
                return Optional.of(urlAsObject.toString());
            }
        }
        return Optional.empty();
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
