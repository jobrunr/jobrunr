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

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

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
        final Optional<Method> urlMethod = Arrays.stream(dataSource.getClass().getMethods())
                .filter(m -> "getUrl".equals(m.getName()) || "getJdbcUrl".equals(m.getName()))
                .findFirst();

        if (urlMethod.isPresent()) {
            return Optional.of(urlMethod.get().invoke(dataSource).toString());
        }
        return Optional.empty();
    }

    private static Optional<String> getUrlViaField(DataSource dataSource) throws ReflectiveOperationException {
        final Optional<Field> urlField = Arrays.stream(dataSource.getClass().getDeclaredFields())
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

    private static JobRunrException unsupportedDataSourceException(DataSource dataSource) {
        return new JobRunrException("Are you running an unsupported DataSource or Database? Please check the documentation. If you think this is wrong, please open an issue using the following url: https://github.com/jobrunr/jobrunr/issues/new?template=bug_report.md&title=%5BBUG%5D5%20-%20missing%20DataSource%20" + dataSource.getClass().getName());
    }

    private static JobRunrException unsupportedDataSourceException(String jdbcUrl) {
        return new JobRunrException("Are you running an unsupported DataSource or Database? Please check the documentation. If you think this is wrong, please open an issue using the following url: https://github.com/jobrunr/jobrunr/issues/new?template=bug_report.md&title=%5BBUG%5D5%20-%20missing%20DataSource%20" + jdbcUrl);
    }
}
