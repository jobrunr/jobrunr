package org.jobrunr.storage.sql;

import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.sql.common.DatabaseMigrationsProvider;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.common.migrations.SqlMigrationByPath;
import org.jobrunr.storage.sql.mariadb.MariaDbStorageProvider;
import org.jobrunr.storage.sql.mysql.MySqlStorageProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class SqlScriptsTest {

    @Test
    void validateAllMigrationScriptsHaveCorrectSQLCasingAndCorrectFieldCasing() throws IOException {
        allMigrations()
                .forEach(this::validateSqlMigrationScript);
    }

    @Test
    void validateJobStatsViewIsCreatedUsingSqlSecurityInvokerForMySqlAndMariaDb() {
        Stream.of(MySqlStorageProvider.class, MariaDbStorageProvider.class)
                .forEach(this::assertJobStatsViewIsCreatedUsingSqlSecurityInvoker);
    }

    private void assertJobStatsViewIsCreatedUsingSqlSecurityInvoker(Class<? extends SqlStorageProvider> sqlStorageProviderClass) {
        String lastStatementCreatingTheJobStatsView = new DatabaseMigrationsProvider(sqlStorageProviderClass).getMigrations()
                .sorted(comparing(SqlMigration::getFileName))
                .flatMap(this::statementsOf)
                .filter(statement -> statement.trim().startsWith("CREATE") && statement.contains("VIEW jobrunr_jobs_stats"))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("Expected a migration creating the jobrunr_jobs_stats view for " + sqlStorageProviderClass.getSimpleName()));

        assertThat(lastStatementCreatingTheJobStatsView)
                .describedAs("The jobrunr_jobs_stats view of %s must be created using SQL SECURITY INVOKER. Using the default SQL SECURITY DEFINER stores the user that ran the migrations inside the view definition, which breaks the dashboard as soon as the database is restored in an environment where that user does not exist.", sqlStorageProviderClass.getSimpleName())
                .contains("SQL SECURITY INVOKER");
    }

    private Stream<String> statementsOf(SqlMigration sqlMigration) {
        try {
            return Stream.of(sqlMigration.getMigrationSql().split(";"));
        } catch (IOException e) {
            throw new RuntimeException("Could not load SQL file", e);
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

    private List<SqlMigrationByPath> allMigrations() throws IOException {
        Path migrationsFolder = Path.of("src/main/resources/org/jobrunr/storage/sql");
        try (Stream<Path> paths = Files.walk(migrationsFolder)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .map(SqlMigrationByPath::new)
                    .collect(toList());
        }
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
            return migrationSql
                    .replaceAll("(?m)^--.*$", "") // remove all single line sql comments
                    .replaceAll("/\\*.*?\\*/", ""); // remove all multi line sql comments
        } catch (IOException e) {
            throw new RuntimeException("Could not load SQL file", e);
        }
    }

    private int countOccurrences(String text, String substringToCount) {
        return text.split("(?<!')\\b" + substringToCount + "\\b(?!')").length - 1;
    }

    private static final Set<Class<?>> TABLES_WITH_FIELDS = Set.of(
            StorageProviderUtils.Migrations.class,
            StorageProviderUtils.Metadata.class,
            StorageProviderUtils.Jobs.class,
            StorageProviderUtils.RecurringJobs.class,
            StorageProviderUtils.BackgroundJobServers.class,
            StorageProviderUtils.JobStats.class);

    private static final Set<String> SQL_KEYWORDS = Set.of("SELECT", "CREATE", "UNIQUE", "INDEX", "DROP", " VIEW", "REPLACE",
            "FROM", "WHERE", "ON", "AS", "NOT", " NULL", "PRIMARY", "ALTER", "MODIFY", "ADD");
}
