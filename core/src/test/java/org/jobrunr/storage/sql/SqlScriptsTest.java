package org.jobrunr.storage.sql;

import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.sql.common.DatabaseMigrationsProvider;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.common.migrations.SqlMigrationByPath;
import org.jobrunr.storage.sql.db2.DB2StorageProvider;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.jobrunr.storage.sql.mariadb.MariaDbStorageProvider;
import org.jobrunr.storage.sql.mysql.MySqlStorageProvider;
import org.jobrunr.storage.sql.oracle.OracleStorageProvider;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.jobrunr.storage.sql.sqlite.SqLiteStorageProvider;
import org.jobrunr.storage.sql.sqlserver.SQLServerStorageProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;

public class SqlScriptsTest {

    @Test
    void validateAllMigrationScriptsHaveCorrectSQLCasingAndCorrectFieldCasing() {
        for (Class<? extends SqlStorageProvider> sqlStorageProviderClass : STORAGE_PROVIDERS) {
            migrationsFor(sqlStorageProviderClass)
                    .forEach(this::validateSqlMigrationScript);
        }
    }

    void validateSqlMigrationScript(SqlMigrationByPath sqlMigration) {
        assertSQLKeywordsHaveCorrectCasing(sqlMigration);
        assertColumnNamingHaveCorrectCasing(sqlMigration);
    }

    private void assertSQLKeywordsHaveCorrectCasing(SqlMigrationByPath sqlMigration) {
        SQL_KEYWORDS.forEach(sqlKeyword -> testNamingAndCasingForSqlKeyword(sqlMigration, sqlKeyword));
    }

    private void assertColumnNamingHaveCorrectCasing(SqlMigrationByPath sqlMigration) {
        TABLES_WITH_FIELDS.forEach(tableWithFields -> testNamingAndCasingForAllFieldsDefinedIn(sqlMigration, tableWithFields));
    }

    private Stream<SqlMigrationByPath> migrationsFor(Class<? extends SqlStorageProvider> sqlStorageProviderClass) {
        return new DatabaseMigrationsProvider(sqlStorageProviderClass)
                .getMigrations()
                .map(SqlMigrationByPath.class::cast)
                .sorted(comparing(SqlMigration::getFileName));
    }

    private void testNamingAndCasingForSqlField(SqlMigrationByPath sqlMigration, String storageProviderReference, String fieldValue) {
        testCasingForString(sqlMigration, fieldValue, "Found invalid casing in " + sqlMigration + " for field " + storageProviderReference + " with value '" + fieldValue + "'");
    }

    private void testNamingAndCasingForSqlKeyword(SqlMigrationByPath sqlMigration, String sqlKeyword) {
        testCasingForString(sqlMigration, sqlKeyword, "Found invalid casing in " + sqlMigration + " for sql clause '" + sqlKeyword + "'");
    }

    private void testCasingForString(SqlMigrationByPath sqlMigration, String string, String describedAsErrorMessage) {
        String sqlMigrationScript = getMigrationScriptWithoutComments(sqlMigration);

        if (!sqlMigrationScript.toLowerCase().contains(string.toLowerCase())) return;

        int countWithoutCasing = countOccurrences(sqlMigrationScript.toLowerCase(), string.toLowerCase());
        int countWithCasing = countOccurrences(sqlMigrationScript, string);
        assertThat(countWithoutCasing)
                .describedAs(describedAsErrorMessage)
                .isEqualTo(countWithCasing);
    }

    private void testNamingAndCasingForAllFieldsDefinedIn(SqlMigrationByPath sqlMigration, Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            try {
                if (isNotPublicStatic(field)) continue;

                String fieldName = field.getName();
                String fieldValue = field.get(clazz).toString();
                testNamingAndCasingForSqlField(sqlMigration, clazz.getSimpleName() + "." + fieldName, fieldValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean isNotPublicStatic(Field field) {
        int modifiers = field.getModifiers();
        return !(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers));
    }

    private String getMigrationScriptWithoutComments(SqlMigration sqlMigration) {
        try {
            String migrationSql = sqlMigration.getMigrationSql();
            return migrationSql.replaceAll("(?m)^--.*$", ""); // remove all sql comments
        } catch (IOException e) {
            throw new RuntimeException("Could not load SQL file", e);
        }
    }

    private int countOccurrences(String text, String substringToCount) {
        return text.split("(?<!')\\b" + substringToCount + "\\b(?!')").length - 1;
    }

    public static final Set<Class<? extends SqlStorageProvider>> STORAGE_PROVIDERS = Set.of(
            DB2StorageProvider.class,
            H2StorageProvider.class,
            MariaDbStorageProvider.class,
            MySqlStorageProvider.class,
            OracleStorageProvider.class,
            PostgresStorageProvider.class,
            SqLiteStorageProvider.class,
            SQLServerStorageProvider.class);

    public static final Set<Class<?>> TABLES_WITH_FIELDS = Set.of(
            StorageProviderUtils.Migrations.class,
            StorageProviderUtils.Metadata.class,
            StorageProviderUtils.Jobs.class,
            StorageProviderUtils.RecurringJobs.class,
            StorageProviderUtils.BackgroundJobServers.class,
            StorageProviderUtils.JobStats.class);

    public static final Set<String> SQL_KEYWORDS = Set.of("SELECT", "CREATE", "UNIQUE", "INDEX", "DROP", " VIEW", "REPLACE",
            "FROM", "WHERE", "ON", "AS", "NOT", " NULL", "PRIMARY", "ALTER", "MODIFY", "ADD");
}
