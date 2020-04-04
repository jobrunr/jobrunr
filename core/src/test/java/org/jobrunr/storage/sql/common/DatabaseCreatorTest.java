package org.jobrunr.storage.sql.common;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.storage.sql.mariadb.MariaDbStorageProviderStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DatabaseCreatorTest {

    public static final String SQLITE_DB = "/tmp/jobrunr-test.db";

    @BeforeEach
    public void setupJobStorageProvider() throws IOException {
        JobRunr.configure();
        Files.deleteIfExists(Paths.get(SQLITE_DB));
    }

    @Test
    public void testSqlLiteMigrations() {
        final DatabaseCreator databaseCreator = new DatabaseCreator(createDataSource("jdbc:sqlite:" + SQLITE_DB));
        assertThatCode(databaseCreator::runMigrations).doesNotThrowAnyException();
    }

    @Test
    public void testDatabaseSpecificMigrations() {
        final DataSource dataSourceMock = Mockito.mock(DataSource.class);
        final DatabaseCreator databaseCreator = new DatabaseCreator(dataSourceMock, new MariaDbStorageProviderStub(dataSourceMock));
        final Stream<Path> databaseSpecificMigrations = databaseCreator.getDatabaseSpecificMigrations();
        assertThat(databaseSpecificMigrations).anyMatch(path -> path.toString().contains("mariadb"));
    }

    private static SQLiteDataSource createDataSource(String url) {
        SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
        sqLiteDataSource.setUrl(url);
        return sqLiteDataSource;
    }

}