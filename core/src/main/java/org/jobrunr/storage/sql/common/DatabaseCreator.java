package org.jobrunr.storage.sql.common;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;
import static org.jobrunr.utils.ClassPathUtils.listAllChildrenOnClasspath;

public class DatabaseCreator {

    private static Logger LOGGER = LoggerFactory.getLogger(DatabaseCreator.class);

    private final DataSource dataSource;
    private SqlStorageProvider sqlStorageProvider;

    protected DatabaseCreator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DatabaseCreator(DataSource dataSource, SqlStorageProvider sqlStorageProvider) {
        this.dataSource = dataSource;
        this.sqlStorageProvider = sqlStorageProvider;
    }

    public void runMigrations() {
        getMigrations()
                .filter(path -> path.toString().endsWith(".sql"))
                .sorted(comparing(p -> p.getFileName().toString()))
                .filter(this::isNewMigration)
                .forEach(this::runMigration);
    }

    protected Stream<Path> getMigrations() {
        final Map<String, Path> commonMigrations = listAllChildrenOnClasspath(DatabaseCreator.class, "migrations").collect(toMap(p -> p.getFileName().toString(), p -> p));
        final Map<String, Path> databaseSpecificMigrations = getDatabaseSpecificMigrations().collect(toMap(p -> p.getFileName().toString(), p -> p));

        final HashMap<String, Path> actualMigrations = new HashMap<>(commonMigrations);
        actualMigrations.putAll(databaseSpecificMigrations);

        return actualMigrations.values().stream();
    }

    protected Stream<Path> getDatabaseSpecificMigrations() {
        if (sqlStorageProvider != null) {
            return listAllChildrenOnClasspath(sqlStorageProvider.getClass(), "migrations");
        }
        return Stream.empty();
    }

    protected void runMigration(Path path) {
        LOGGER.info("Running migration {}", path);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);
            runMigrationStatement(conn, path);
            updateMigrationsTable(conn, path);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(new IllegalStateException("Error running database migration " + path.getFileName(), e));
        }
    }

    protected void runMigrationStatement(Connection connection, Path path) throws IOException, SQLException {
        try (final Statement stmt = connection.createStatement()) {
            final String sql = new String(Files.readAllBytes(path));
            for (String statement : sql.split(";")) {
                stmt.addBatch(statement);
            }
            stmt.executeBatch();
        }
    }

    protected void updateMigrationsTable(Connection connection, Path path) throws SQLException {
        try (PreparedStatement pSt = connection.prepareStatement("insert into jobrunr_migrations values (?, ?, ?)")) {
            pSt.setString(1, UUID.randomUUID().toString());
            pSt.setString(2, path.getFileName().toString());
            pSt.setString(3, LocalDateTime.now().toString());
            pSt.execute();
        }
    }

    private boolean isNewMigration(Path path) {
        return !isMigrationApplied(path);
    }

    protected boolean isMigrationApplied(Path path) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);
            try (PreparedStatement pSt = conn.prepareStatement("select count(*) from jobrunr_migrations where script = ?")) {
                pSt.setString(1, path.getFileName().toString());
                try (ResultSet rs = pSt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        return count == 1;
                    }
                    return false;
                }
            }
        } catch (Exception becauseTableDoesNotExist) {
            return false;
        }
    }
}
