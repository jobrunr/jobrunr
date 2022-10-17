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
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

public class DatabaseCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreator.class);
    private static final String[] JOBRUNR_TABLES = new String[]{"jobrunr_jobs", "jobrunr_recurring_jobs", "jobrunr_backgroundjobservers", "jobrunr_metadata"};

    private final ConnectionProvider connectionProvider;
    private final TablePrefixStatementUpdater tablePrefixStatementUpdater;
    private final DatabaseMigrationsProvider databaseMigrationsProvider;

    public static void main(String[] args) {
        if(args.length < 3) {
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
        getMigrations()
                .filter(migration -> migration.getFileName().endsWith(".sql"))
                .sorted(comparing(SqlMigration::getFileName))
                .filter(this::isNewMigration)
                .forEach(this::runMigration);
    }

    public void validateTables() {
        try (final Connection conn = getConnection();
             final Transaction tran = new Transaction(conn, false);
             final Statement pSt = conn.createStatement()) {
            for (String table : JOBRUNR_TABLES) {
                try (ResultSet rs = pSt.executeQuery("select count(*) from " + tablePrefixStatementUpdater.getFQTableName(table))) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
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

    protected void runMigration(SqlMigration migration) {
        LOGGER.info("Running migration {}", migration);
        try (final Connection conn = getConnection(); final Transaction tran = new Transaction(conn, false)) {
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
                stmt.execute(tablePrefixStatementUpdater.updateStatement(statement).trim());
            }
        }
    }

    protected void updateMigrationsTable(Connection connection, SqlMigration migration) throws SQLException {
        try (PreparedStatement pSt = connection.prepareStatement("insert into " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " values (?, ?, ?)")) {
            pSt.setString(1, UUID.randomUUID().toString());
            pSt.setString(2, migration.getFileName());
            pSt.setString(3, LocalDateTime.now().toString());
            pSt.execute();
        }
    }

    private boolean isNewMigration(SqlMigration migration) {
        return !isMigrationApplied(migration);
    }

    protected boolean isMigrationApplied(SqlMigration migration) {
        try (final Connection conn = getConnection();
             final Transaction tran = new Transaction(conn, false);
             final PreparedStatement pSt = conn.prepareStatement("select count(*) from " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " where script = ?")) {
            boolean result = false;
            pSt.setString(1, migration.getFileName());
            try (ResultSet rs = pSt.executeQuery()) {
                if (rs.next()) {
                    int numberOfRows = rs.getInt(1);
                    if(numberOfRows > 1) {
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
                try(Connection connection = connectionProvider.getConnection()) {
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
