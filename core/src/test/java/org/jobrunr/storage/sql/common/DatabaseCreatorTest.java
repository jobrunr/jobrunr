package org.jobrunr.storage.sql.common;

import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.JobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.storage.sql.common.migrations.DefaultSqlMigrationProvider;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DatabaseCreatorTest {

    static final String SQLITE_DB1 = "/tmp/jobrunr-test.db";
    static final String SQLITE_DB2 = "/tmp/jobrunr-failing-test.db";

    @BeforeEach
    void setupJobStorageProvider() throws IOException {
        JobRunr.configure();
        Files.deleteIfExists(Paths.get(SQLITE_DB1));
    }

    @Test
    void testSqlLiteMigrationsUsingMainMethod() {
        assertThatCode(() -> DatabaseCreator.main(new String[]{"jdbc:sqlite:" + SQLITE_DB1, "", ""})).doesNotThrowAnyException();
    }

    @Test
    void testSqlLiteMigrations() {
        final DatabaseCreator databaseCreator = new DatabaseCreator(createDataSource("jdbc:sqlite:" + SQLITE_DB1));
        assertThatCode(databaseCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(databaseCreator::validateTables).doesNotThrowAnyException();
    }

    @Test
    void testSqlLiteMigrationsAllMigrationsApplied() {
        DefaultSqlMigrationProvider sqlMigrationProvider = new DefaultSqlMigrationProvider();
        List<SqlMigration> migrations = sqlMigrationProvider.getMigrations(DatabaseCreator.class);

        final DatabaseCreator databaseCreator = new DatabaseCreator(createDataSource("jdbc:sqlite:" + SQLITE_DB1));
        assertThatCode(databaseCreator::runMigrations).doesNotThrowAnyException();

        migrations.forEach(migration -> assertThat(databaseCreator.isMigrationApplied(migration))
                .describedAs("Expecting %s to be applied", migration)
                .isTrue());
    }

    @Test
    void testValidateWithoutTables() {
        final DatabaseCreator databaseCreator = new DatabaseCreator(createDataSource("jdbc:sqlite:" + SQLITE_DB2));
        assertThatThrownBy(databaseCreator::validateTables).isInstanceOf(JobRunrException.class);
    }

    @Test
    void testH2MigrationsWithSchema() {
        final JdbcDataSource dataSource = createH2DataSource("jdbc:h2:/tmp/test;INIT=CREATE SCHEMA IF NOT EXISTS the_schema");
        final DatabaseCreator databaseCreator = new DatabaseCreator(dataSource, "the_schema.prefix_", H2StorageProvider.class);
        assertThatCode(databaseCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(databaseCreator::validateTables).doesNotThrowAnyException();
    }

    @Test
    void testH2ValidateWithTablesInWrongSchema() {
        final JdbcDataSource dataSource = createH2DataSource("jdbc:h2:/tmp/test;INIT=CREATE SCHEMA IF NOT EXISTS schema1\\;CREATE SCHEMA IF NOT EXISTS schema2");
        final DatabaseCreator databaseCreatorForSchema1 = new DatabaseCreator(dataSource, "schema1.prefix_", H2StorageProvider.class);
        databaseCreatorForSchema1.runMigrations();
        final DatabaseCreator databaseCreatorForSchema2 = new DatabaseCreator(dataSource, "schema2.prefix_", H2StorageProvider.class);
        assertThatThrownBy(databaseCreatorForSchema2::validateTables).isInstanceOf(JobRunrException.class);
    }

    @Test
    void testMigrationIsNotDoneMoreThanOnce() {
        final JdbcDataSource dataSource = createH2DataSource("jdbc:h2:mem:/test;DB_CLOSE_DELAY=-1");
        final DatabaseCreator databaseCreator = Mockito.spy(new DatabaseCreator(dataSource, H2StorageProvider.class));

        assertThatCode(databaseCreator::runMigrations).doesNotThrowAnyException();

        insertExtraMigrationInDB(dataSource, databaseCreator);
        Mockito.reset(databaseCreator);

        assertThatCode(databaseCreator::runMigrations)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A migration was applied multiple times (probably because it took too long and the process was killed). " +
                        "Please cleanup the migrations_table and remove duplicate entries.");

        verify(databaseCreator, never()).runMigration(any());
    }

    private JdbcDataSource createH2DataSource(String url) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(url);
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }

    private static SQLiteDataSource createDataSource(String url) {
        SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
        sqLiteDataSource.setUrl(url);
        return sqLiteDataSource;
    }

    private void insertExtraMigrationInDB(DataSource dataSource, DatabaseCreator databaseCreator) {
        try(Connection connection = dataSource.getConnection()) {
            SqlMigration migration = mock(SqlMigration.class);
            when(migration.getFileName()).thenReturn("v014__improve_job_stats.sql");
            databaseCreator.updateMigrationsTable(connection, migration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}