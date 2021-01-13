package org.jobrunr.storage.sql.common;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public class DatabaseCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreator.class);

    private final ConnectionProvider connectionProvider;
    private final DatabaseMigrationsProvider databaseMigrationsProvider;

    public static void main(String[] args) {
        String url = args[0];
        String userName = args[1];
        String password = args[2];

        try {
            System.out.println("==========================================================");
            System.out.println("================== JobRunr Table Creator =================");
            System.out.println("==========================================================");
            new DatabaseCreator(() -> DriverManager.getConnection(url, userName, password), new SqlStorageProviderFactory().getStorageProviderClassByJdbcUrl(url)).runMigrations();
            System.out.println("Successfully created all tables!");
        } catch (Exception e) {
            System.out.println("An error occurred: ");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            System.out.println(exceptionAsString);
        }
    }

    protected DatabaseCreator(DataSource dataSource) {
        this(dataSource, null);
    }

    public DatabaseCreator(DataSource dataSource, Class<? extends SqlStorageProvider> sqlStorageProviderClass) {
        this(dataSource::getConnection, sqlStorageProviderClass);
    }

    public DatabaseCreator(ConnectionProvider connectionProvider, Class<? extends SqlStorageProvider> sqlStorageProviderClass) {
        this.connectionProvider = connectionProvider;
        this.databaseMigrationsProvider = new DatabaseMigrationsProvider(sqlStorageProviderClass);
    }

    public void runMigrations() {
        getMigrations()
                .filter(migration -> migration.getFileName().endsWith(".sql"))
                .sorted(comparing(migration -> migration.getFileName()))
                .filter(this::isNewMigration)
                .forEach(this::runMigration);
    }

    public void validateTables() {
        String[] tables = {"jobrunr_jobs", "jobrunr_recurring_jobs", "jobrunr_backgroundjobservers", "jobrunr_jobs_stats"};
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(true);
            try (Statement pSt = conn.createStatement()) {
                for (String table : tables) {
                    try (ResultSet rs = pSt.executeQuery("select count(*) from " + table)) {
                        if (rs.next()) {
                            int count = rs.getInt(1);
                        }
                    }
                }
            }
        } catch (Exception becauseTableDoesNotExist) {
            throw new JobRunrException("Not all required tables are available by JobRunr!");
        }
    }

    protected Stream<Migration> getMigrations() {
        return databaseMigrationsProvider.getMigrations();
    }

    protected void runMigration(Migration migration) {
        LOGGER.info("Running migration {}", migration);
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(true);
            runMigrationStatement(conn, migration);
            updateMigrationsTable(conn, migration);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(new IllegalStateException("Error running database migration " + migration.getFileName(), e));
        }
    }

    protected void runMigrationStatement(Connection connection, Migration migration) throws IOException, SQLException {
        final String sql = migration.getMigrationSql();
        for (String statement : sql.split(";")) {
            try (final Statement stmt = connection.createStatement()) {
                stmt.execute(statement);
            }
        }
    }

    protected void updateMigrationsTable(Connection connection, Migration migration) throws SQLException {
        try (PreparedStatement pSt = connection.prepareStatement("insert into jobrunr_migrations values (?, ?, ?)")) {
            pSt.setString(1, UUID.randomUUID().toString());
            pSt.setString(2, migration.getFileName());
            pSt.setString(3, LocalDateTime.now().toString());
            pSt.execute();
        }
    }

    private boolean isNewMigration(Migration migration) {
        return !isMigrationApplied(migration);
    }

    protected boolean isMigrationApplied(Migration migration) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(true);
            try (PreparedStatement pSt = conn.prepareStatement("select count(*) from jobrunr_migrations where script = ?")) {
                pSt.setString(1, migration.getFileName());
                try (ResultSet rs = pSt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) == 1;
                    }
                    return false;
                }
            }
        } catch (Exception becauseTableDoesNotExist) {
            return false;
        }
    }

    private Connection getConnection() {
        try {
            return connectionProvider.getConnection();
        } catch (SQLException exception) {
            throw JobRunrException.shouldNotHappenException(exception);
        }
    }

    @FunctionalInterface
    private interface ConnectionProvider {

        Connection getConnection() throws SQLException;

    }

}
