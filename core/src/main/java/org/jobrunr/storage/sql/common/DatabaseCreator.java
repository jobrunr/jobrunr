package org.jobrunr.storage.sql.common;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.db.Transaction;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.common.tables.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

public class DatabaseCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreator.class);
    private static final String[] JOBRUNR_TABLES = new String[]{"jobrunr_jobs", "jobrunr_recurring_jobs", "jobrunr_backgroundjobservers", "jobrunr_metadata"};
    private static final String NULL_UUID = "00000000-0000-0000-0000-000000000000";

    private final ConnectionProvider connectionProvider;
    private final TablePrefixStatementUpdater tablePrefixStatementUpdater;
    private final DatabaseMigrationsProvider databaseMigrationsProvider;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Error: insufficient arguments");
            System.out.println();
            System.out.println("usage: java -cp jobrunr-${jobrunr.version}.jar org.jobrunr.storage.sql.common.DatabaseCreator {jdbcUrl} {userName} {password} ({tablePrefix})");
            return;
        }
        String url = args[0];
        String userName = args[1];
        String password = args[2];
        String tablePrefix = args.length >= 4 ? args[3] : null;

        try {
            System.out.println("==========================================================");
            System.out.println("================== JobRunr Table Creator =================");
            System.out.println("==========================================================");
            new DatabaseCreator(() -> DriverManager.getConnection(url, userName, password), tablePrefix, new SqlStorageProviderFactory().getStorageProviderClassByJdbcUrl(url)).runMigrations();
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
        this(dataSource, null, null);
    }

    protected DatabaseCreator(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, null);
    }

    public DatabaseCreator(DataSource dataSource, Class<? extends SqlStorageProvider> sqlStorageProviderClass) {
        this(dataSource::getConnection, null, sqlStorageProviderClass);
    }

    public DatabaseCreator(DataSource dataSource, String tablePrefix, Class<? extends SqlStorageProvider> sqlStorageProviderClass) {
        this(dataSource::getConnection, tablePrefix, sqlStorageProviderClass);
    }

    public DatabaseCreator(ConnectionProvider connectionProvider, String tablePrefix, Class<? extends SqlStorageProvider> sqlStorageProviderClass) {
        this.connectionProvider = connectionProvider;
        this.tablePrefixStatementUpdater = getStatementUpdater(tablePrefix, connectionProvider);
        this.databaseMigrationsProvider = new DatabaseMigrationsProvider(sqlStorageProviderClass);
    }

    public void runMigrations() {
        runMigrations(getMigrations()
                .filter(migration -> migration.getFileName().endsWith(".sql"))
                .sorted(comparing(SqlMigration::getFileName))
                .filter(this::isNewMigration)
                .collect(Collectors.toList())
        );
    }

    public void validateTables() {
        try (final Connection conn = getConnection();
             final Transaction tran = new Transaction(conn);
             final Statement pSt = conn.createStatement()) {
            for (String table : JOBRUNR_TABLES) {
                try (ResultSet rs = pSt.executeQuery("select count(*) from " + tablePrefixStatementUpdater.getFQTableName(table))) {
                    if (rs.next()) {
                        int ignored = rs.getInt(1);
                    }
                }
            }
            tran.commit();
        } catch (Exception becauseTableDoesNotExist) {
            throw new JobRunrException("Not all required tables are available by JobRunr!");
        }
    }

    protected Stream<SqlMigration> getMigrations() {
        return databaseMigrationsProvider.getMigrations();
    }

    private void runMigrations(List<SqlMigration> migrations) {
        if (migrations.isEmpty()) return;

        if (isMigrationsTableMigration(migrations.get(0))) {
            createMigrationsTable(migrations.remove(0));
        }

        if (lockMigrationsTable()) {
            try {
                migrations.forEach(this::runMigration);
            } finally {
                removeMigrationsTableLock();
            }
        } else {
            waitUntilMigrationsAreDone();
        }
    }

    protected void runMigration(SqlMigration migration) {
        LOGGER.info("Running migration {}", migration);
        try (final Connection conn = getConnection(); final Transaction tran = new Transaction(conn)) {
            if (!isEmptyMigration(migration)) {
                runMigrationStatement(conn, migration);
            }
            updateMigrationsTable(conn, migration);
            tran.commit();
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(new IllegalStateException("Error running database migration " + migration.getFileName(), e));
        }
    }

    private boolean isEmptyMigration(SqlMigration migration) throws IOException {
        return migration.getMigrationSql().startsWith("-- Empty migration");
    }


    protected void runMigrationStatement(Connection connection, SqlMigration migration) throws IOException, SQLException {
        final String sql = migration.getMigrationSql();
        for (String statement : sql.split(";")) {
            try (final Statement stmt = connection.createStatement()) {
                String updatedStatement = tablePrefixStatementUpdater.updateStatement(statement).trim();
                stmt.execute(updatedStatement);
            }
        }
    }

    private void createMigrationsTable(SqlMigration migration) {
        try {
            runMigration(migration);
        } catch (Exception e) {
            LOGGER.info("Error when creating the migrations table", e);
            // the table already exists, or we'll fail in the next steps
        }
    }

    protected void updateMigrationsTable(Connection connection, SqlMigration migration) throws SQLException {
        updateMigrationsTable(connection, UUID.randomUUID().toString(), migration.getFileName());
    }

    private void updateMigrationsTable(Connection connection, String id, String filename) throws SQLException {
        try (PreparedStatement pSt = connection.prepareStatement("insert into " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " values (?, ?, ?)")) {
            pSt.setString(1, id);
            pSt.setString(2, filename);
            pSt.setString(3, LocalDateTime.now().toString());
            pSt.execute();
        }
    }

    private boolean lockMigrationsTable() {
        LOGGER.info("Trying to lock migrations table...");
        try (final Connection conn = getConnection(); final Transaction tran = new Transaction(conn)) {
            updateMigrationsTable(conn, NULL_UUID, "1");
            tran.commit();
        } catch (Exception e) {
            LOGGER.info("Migrations table is already locked.", e);
            return false;
        }
        LOGGER.info("Migrations table is locked.");
        return true;
    }

    private void removeMigrationsTableLock() {
        LOGGER.info("Removing lock on migrations table...");
        try (final Connection conn = getConnection();
             final Transaction tran = new Transaction(conn);
             PreparedStatement pSt = conn.prepareStatement("delete from " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " where id = ?")) {
            pSt.setString(1, NULL_UUID);
            pSt.execute();
            tran.commit();
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(new IllegalStateException("Error removing lock from migrations table", e));
        }
        LOGGER.info("The lock has been removed from migrations table.");
    }

    private void waitUntilMigrationsAreDone() {
        LOGGER.info("Waiting for the end of database migrations...");
        try {
            while (migrationsAreOnGoing()) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(new IllegalStateException("Error waiting for the end of database migrations", e));
        }
    }

    private boolean migrationsAreOnGoing() throws SQLException {
        try (
                final Connection conn = getConnection();
                final PreparedStatement pSt = conn.prepareStatement("select 1 from " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " where id = ?")
        ) {
            pSt.setString(1, NULL_UUID);
            ResultSet resultSet = pSt.executeQuery();
            return !resultSet.next();
        }
    }

    private boolean isNewMigration(SqlMigration migration) {
        return !isMigrationApplied(migration);
    }

    private boolean isMigrationsTableMigration(SqlMigration migration) {
        return migration.getFileName().endsWith("v000__create_migrations_table.sql");
    }

    protected boolean isMigrationApplied(SqlMigration migration) {
        try (final Connection conn = getConnection();
             final Transaction tran = new Transaction(conn);
             final PreparedStatement pSt = conn.prepareStatement("select count(*) from " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " where script = ?")) {
            boolean result = false;
            pSt.setString(1, migration.getFileName());
            try (ResultSet rs = pSt.executeQuery()) {
                if (rs.next()) {
                    int numberOfRows = rs.getInt(1);
                    if (numberOfRows > 1) {
                        throw new IllegalStateException("A migration was applied multiple times (probably because it took too long and the process was killed). " +
                                "Please cleanup the migrations_table and remove duplicate entries.");
                    }
                    result = numberOfRows >= 1;
                }
            }
            tran.commit();
            return result;
        } catch (SQLException becauseTableDoesNotExist) {
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

    private TablePrefixStatementUpdater getStatementUpdater(String tablePrefix, ConnectionProvider connectionProvider) {
        try {
            if (isNullOrEmpty(tablePrefix)) {
                return new NoOpTablePrefixStatementUpdater();
            } else {
                try (Connection connection = connectionProvider.getConnection()) {
                    final String databaseProductName = connection.getMetaData().getDatabaseProductName();
                    if ("Oracle".equals(databaseProductName) || databaseProductName.startsWith("DB2")) {
                        return new OracleAndDB2TablePrefixStatementUpdater(tablePrefix);
                    } else if ("Microsoft SQL Server".equals(databaseProductName)) {
                        return new SqlServerDatabaseTablePrefixStatementUpdater(tablePrefix);
                    } else {
                        return new AnsiDatabaseTablePrefixStatementUpdater(tablePrefix);
                    }
                }
            }
        } catch (SQLException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }
}
