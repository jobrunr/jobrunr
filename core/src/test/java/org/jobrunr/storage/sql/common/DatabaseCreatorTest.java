package org.jobrunr.storage.sql.common;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.JobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.storage.sql.common.migrations.DefaultSqlMigrationProvider;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;

import static ch.qos.logback.LoggerAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        final JdbcDataSource dataSource = createH2DataSource("jdbc:h2:/tmp/test-wrong-schema;INIT=CREATE SCHEMA IF NOT EXISTS schema1\\;CREATE SCHEMA IF NOT EXISTS schema2");
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
                        "Please verify your migrations manually, cleanup the migrations_table and remove duplicate entries.");

        verify(databaseCreator, never()).runMigration(any());
    }

    @Test
    void testMigrationsAreNotRunningConcurrently() throws InterruptedException {
        final JdbcDataSource dataSource = createH2DataSource("jdbc:h2:mem:/test;DB_CLOSE_DELAY=-1");
        final DatabaseCreator databaseCreator1 = Mockito.spy(new DatabaseCreator(dataSource, H2StorageProvider.class));
        final DatabaseCreator databaseCreator2 = Mockito.spy(new DatabaseCreator(dataSource, H2StorageProvider.class));
        final DatabaseCreator databaseCreator3 = Mockito.spy(new DatabaseCreator(dataSource, H2StorageProvider.class));

        final ListAppender<ILoggingEvent> loggerDbCreator = LoggerAssert.initFor(databaseCreator1);

        Thread t1 = new Thread(databaseCreator1::runMigrations);
        Thread t2 = new Thread(databaseCreator2::runMigrations);
        Thread t3 = new Thread(databaseCreator3::runMigrations);

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        assertThat(loggerDbCreator)
                .hasDebugMessageContaining("Successfully locked the migrations table.", 1)
                .hasDebugMessageContaining("Too late... Another DatabaseCreator is performing the migrations.", 2)
                .hasDebugMessageContaining("The lock has been removed from migrations table.", 1)
                .hasInfoMessageContaining("Running migration")
                .hasInfoMessageContaining("Waiting for database migrations to finish...", 2);
    }

    // why: dropping and creating new indexes can take a good amount of time
    @Test
    void migrationsThatTakeLongUpdateMigrationLockAndDoNotFail() throws InterruptedException {
        final JdbcDataSource dataSource = createH2DataSource("jdbc:h2:mem:/test-long-migration;DB_CLOSE_DELAY=-1");
        final DatabaseCreator databaseCreator1 = Mockito.spy(new DatabaseCreator(dataSource, H2StorageProvider.class));
        final DatabaseCreator databaseCreator2 = Mockito.spy(new DatabaseCreator(dataSource, H2StorageProvider.class));

        delaySqlMigration(databaseCreator1, "v001__", 12_000);
        delaySqlMigration(databaseCreator2, "v001__", 12_000);

        final ListAppender<ILoggingEvent> loggerDbCreator = LoggerAssert.initFor(databaseCreator1);

        Thread t1 = new Thread(databaseCreator1::runMigrations);
        Thread t2 = new Thread(databaseCreator2::runMigrations);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertThat(loggerDbCreator)
                .hasDebugMessageContaining("Updating lock on migrations table...", 2)
                .hasDebugMessageContaining("The lock has been removed from migrations table.", 1)
                .hasInfoMessageContaining("Waiting for database migrations to finish...", 1);
    }

    @Test
    void checksThatMigrationsTableIsNoLongerLockedWhenThereAreNoNewMigrations() {
        final JdbcDataSource dataSource = createH2DataSource("jdbc:h2:mem:/test-always-checks-lock;DB_CLOSE_DELAY=-1");
        final DatabaseCreator databaseCreator = Mockito.spy(new DatabaseCreator(dataSource, H2StorageProvider.class));
        final ListAppender<ILoggingEvent> loggerDbCreator = LoggerAssert.initFor(databaseCreator);

        assertThatCode(databaseCreator::runMigrations).doesNotThrowAnyException();
        assertThat(loggerDbCreator)
                .hasDebugMessageContaining("Successfully locked the migrations table.", 1)
                .hasDebugMessageContaining("The lock has been removed from migrations table.", 1)
                .hasInfoMessageContaining("Waiting for database migrations to finish...", 0);

        assertThatCode(databaseCreator::runMigrations).doesNotThrowAnyException();
        assertThat(loggerDbCreator)
                .hasDebugMessageContaining("Successfully locked the migrations table.", 1)
                .hasDebugMessageContaining("The lock has been removed from migrations table.", 1)
                .hasDebugMessageContaining("No migrations to run.", 1);
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
        try (Connection connection = dataSource.getConnection()) {
            SqlMigration migration = mock(SqlMigration.class);
            when(migration.getFileName()).thenReturn("v014__improve_job_stats.sql");
            databaseCreator.updateMigrationsTable(connection, migration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void delaySqlMigration(DatabaseCreator databaseCreator, String migrationPrefix, long delayInMillis) {
        doAnswer((Answer<Void>) invocation -> {
            invocation.callRealMethod();
            SqlMigration migration = invocation.getArgument(0);
            if (migration.getFileName().startsWith(migrationPrefix)) {
                Thread.sleep(delayInMillis);
            }
            return null;
        }).when(databaseCreator).runMigration(any(SqlMigration.class));
    }
}