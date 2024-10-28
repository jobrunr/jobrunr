package org.jobrunr.storage.sql.common;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.db.Transaction;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.common.tables.AnsiDatabaseTablePrefixStatementUpdater;
import org.jobrunr.storage.sql.common.tables.NoOpTablePrefixStatementUpdater;
import org.jobrunr.storage.sql.common.tables.OracleAndDB2TablePrefixStatementUpdater;
import org.jobrunr.storage.sql.common.tables.SqlServerDatabaseTablePrefixStatementUpdater;
import org.jobrunr.storage.sql.common.tables.TablePrefixStatementUpdater;
import org.jobrunr.utils.StringUtils;
import org.jobrunr.utils.annotations.VisibleFor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;
import static java.time.Instant.now;
import static java.time.Instant.parse;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

public class DatabaseCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreator.class);
    private static final String[] JOBRUNR_TABLES = new String[]{"jobrunr_jobs", "jobrunr_recurring_jobs", "jobrunr_backgroundjobservers", "jobrunr_metadata"};

    private final ConnectionProvider connectionProvider;
    private final TablePrefixStatementUpdater tablePrefixStatementUpdater;
    private final DatabaseMigrationsProvider databaseMigrationsProvider;
    private final MigrationsTableLocker migrationsTableLocker;

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
        this.migrationsTableLocker = new MigrationsTableLocker(connectionProvider, tablePrefixStatementUpdater);
    }

    public void runMigrations() {
        boolean isMigrationTableMissing = isMigrationsTableMissing();
        List<SqlMigration> migrationsToRun = getMigrations()
                .filter(migration -> migration.getFileName().endsWith(".sql"))
                .sorted(comparing(SqlMigration::getFileName))
                .filter(x -> isMigrationTableMissing || isNewMigration(x))
                .collect(toList());
        runMigrations(migrationsToRun);
    }

    public void validateTables() {
        List<String> expectedTables = stream(JOBRUNR_TABLES).map(tablePrefixStatementUpdater::getFQTableName).map(String::toUpperCase).collect(toList());
        List<String> allTableNames = getAllTableNames();
        expectedTables.removeAll(allTableNames);
        if (!expectedTables.isEmpty()) {
            throw new JobRunrException("Not all required tables are available by JobRunr!");
        }
    }

    private boolean isMigrationsTableMissing() {
        String migrationsFQTableName = tablePrefixStatementUpdater.getFQTableName("JOBRUNR_MIGRATIONS").toUpperCase();
        return getAllTableNames().stream().map(String::toUpperCase).noneMatch(x -> x.contains(migrationsFQTableName));
    }

    @VisibleFor("testing")
    List<String> getAllTableNames() {
        try (final Connection conn = getConnection()) {
            List<String> allTableNames = new ArrayList<>();
            ResultSet tables = conn.getMetaData().getTables(null, null, "%", null);
            while (tables.next()) {
                if (tablePrefixStatementUpdater.getSchema() != null) {
                    String tableSchema = tables.getString("TABLE_SCHEM");
                    String tableName = tables.getString("TABLE_NAME");
                    String completeTableName = Stream.of(tableSchema, tableName).filter(StringUtils::isNotNullOrEmpty).map(String::toUpperCase).collect(joining("."));
                    allTableNames.add(completeTableName);
                } else {
                    String tableName = tables.getString("TABLE_NAME").toUpperCase();
                    allTableNames.add(tableName);
                }
            }
            return allTableNames;
        } catch (SQLException e) {
            throw new StorageException("Unable to query database tables to see if JobRunr Tables were created.", e);
        }
    }

    protected Stream<SqlMigration> getMigrations() {
        return databaseMigrationsProvider.getMigrations();
    }

    private void runMigrations(List<SqlMigration> migrationsToRun) {
        if (migrationsToRun.isEmpty()) {
            migrationsTableLocker.waitUntilMigrationsAreDone();
            return;
        }

        if (isCreateMigrationsTableMigration(migrationsToRun.get(0))) {
            createMigrationsTable(migrationsToRun.remove(0));
        }

        if (migrationsTableLocker.lockMigrationsTable()) {
            try {
                migrationsToRun.forEach(this::runMigration);
            } finally {
                migrationsTableLocker.removeMigrationsTableLock();
            }
        } else {
            migrationsTableLocker.waitUntilMigrationsAreDone();
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
            throw shouldNotHappenException(new IllegalStateException("Error running database migration " + migration.getFileName(), e));
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
            LOGGER.debug("Error when creating the migrations table, it probably already exists.", e);
            // the table already exists, or we'll fail in the next steps
        }
    }

    protected void updateMigrationsTable(Connection connection, SqlMigration migration) throws SQLException {
        try (PreparedStatement pSt = connection.prepareStatement("insert into " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " values (?, ?, ?)")) {
            pSt.setString(1, UUID.randomUUID().toString());
            pSt.setString(2, migration.getFileName());
            pSt.setString(3, now().truncatedTo(MICROS).toString());
            int updateCount = pSt.executeUpdate();
            if (updateCount == 0) throw new IllegalStateException("Could not save migration to migrations table");
        }
    }

    private boolean isNewMigration(SqlMigration migration) {
        return !isMigrationApplied(migration);
    }

    private boolean isCreateMigrationsTableMigration(SqlMigration migration) {
        return migration.getFileName().endsWith("v000__create_migrations_table.sql");
    }

    protected boolean isMigrationApplied(SqlMigration migration) {
        try (final Connection conn = getConnection();
             final PreparedStatement pSt = conn.prepareStatement("select count(*) from " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " where script = ?")) {
            boolean result = false;
            pSt.setString(1, migration.getFileName());
            try (ResultSet rs = pSt.executeQuery()) {
                if (rs.next()) {
                    int numberOfRows = rs.getInt(1);
                    if (numberOfRows > 1) {
                        throw new IllegalStateException("A migration was applied multiple times (probably because it took too long and the process was killed). " +
                                "Please verify your migrations manually, cleanup the migrations_table and remove duplicate entries.");
                    }
                    result = numberOfRows == 1;
                }
            }
            return result;
        } catch (SQLException sqlException) {
            LOGGER.debug("Error checking if migration {} is already applied", migration.getFileName(), sqlException);
            throw new StorageException(sqlException);
        }
    }

    private Connection getConnection() {
        try {
            return connectionProvider.getConnection();
        } catch (SQLException exception) {
            throw shouldNotHappenException(exception);
        }
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
            throw shouldNotHappenException(e);
        }
    }

    @FunctionalInterface
    private interface ConnectionProvider {

        Connection getConnection() throws SQLException;

    }

    private static class MigrationsTableLocker {
        private static final String TABLE_LOCKER_UUID = "00000000-0000-0000-0000-000000000000";
        private static final String TABLE_LOCKER_SCRIPT = "TABLE LOCKER";

        private final ConnectionProvider connectionProvider;
        private final TablePrefixStatementUpdater tablePrefixStatementUpdater;
        private ScheduledExecutorService lockUpdateScheduler;

        public MigrationsTableLocker(ConnectionProvider connectionProvider, TablePrefixStatementUpdater tablePrefixStatementUpdater) {
            this.connectionProvider = connectionProvider;
            this.tablePrefixStatementUpdater = tablePrefixStatementUpdater;
        }

        private boolean lockMigrationsTable() {
            LOGGER.debug("Trying to lock migrations table...");
            try (final Connection conn = getConnection(); final Transaction tran = new Transaction(conn)) {
                insertLock(conn);
                tran.commit();
                LOGGER.debug("Successfully locked the migrations table.");
                startMigrationsTableLockUpdateTimer();
                return true;
            } catch (Exception e) {
                LOGGER.debug("Too late... Another DatabaseCreator is performing the migrations.", e);
                return false;
            }
        }

        private void startMigrationsTableLockUpdateTimer() {
            lockUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
            lockUpdateScheduler.scheduleAtFixedRate(this::updateMigrationsTableLock, 5, 5, TimeUnit.SECONDS);
        }

        private void removeMigrationsTableLock() {
            LOGGER.debug("Removing lock on migrations table...");
            lockUpdateScheduler.shutdown();
            try (final Connection conn = getConnection(); final Transaction tran = new Transaction(conn)) {
                removeLock(conn);
                tran.commit();
            } catch (Exception e) {
                throw shouldNotHappenException(new IllegalStateException("Error removing lock from migrations table", e));
            }
        }

        private void updateMigrationsTableLock() {
            LOGGER.debug("Updating lock on migrations table...");
            try (final Connection conn = getConnection(); final Transaction tran = new Transaction(conn)) {
                updateLock(conn);
                tran.commit();
            } catch (Exception e) {
                throw shouldNotHappenException(new IllegalStateException("Error removing lock from migrations table", e));
            }
        }

        private void waitUntilMigrationsAreDone() {
            LOGGER.info("Waiting for database migrations to finish...");
            try {
                while (isMigrationsTableLocked()) {
                    sleep(2000);
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Server was stopped before all migrations tables were finished.");
                Thread.currentThread().interrupt();
            } catch (SQLException e) {
                throw shouldNotHappenException(e);
            } catch (Exception e) {
                LOGGER.error("Error waiting for database migrations to finish. Manually review your database migrations in the jobrunr_migrations table and then delete the migration lock entry with id '{}' before trying again.", TABLE_LOCKER_UUID, e);
                throw e;
            }
        }

        private boolean isMigrationsTableLocked() throws SQLException {
            try (final Connection conn = getConnection(); final PreparedStatement pSt = conn.prepareStatement("select * from " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " where id = ?")) {
                pSt.setString(1, TABLE_LOCKER_UUID);
                ResultSet rs = pSt.executeQuery();
                if (rs.next()) {
                    // why: we want to confirm that the migrations are still running, see also this::startMigrationsTableLockUpdateTimer()
                    Instant lastTableLockUpdate = parse(rs.getString("installedOn"));
                    if (now().isAfter(lastTableLockUpdate.plus(20, ChronoUnit.SECONDS))) {
                        throw new IllegalStateException("Database migrations have timed out.");
                    }
                    return true;
                }
                return false;
            }
        }

        private void insertLock(Connection connection) throws SQLException {
            try (final PreparedStatement pSt = connection.prepareStatement("insert into " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " values (?, ?, ?)")) {
                pSt.setString(1, TABLE_LOCKER_UUID);
                pSt.setString(2, TABLE_LOCKER_SCRIPT);
                pSt.setString(3, now().truncatedTo(MICROS).toString());
                int updateCount = pSt.executeUpdate();
                if (updateCount == 0) throw new IllegalStateException("Another DatabaseCreator is performing the migrations table.");
            }
        }

        // why: dropping and creating new indexes can take a good amount of time. Here we update the installedOn column so it can be awaited and monitored by other servers.
        private void updateLock(Connection connection) throws SQLException {
            try (final PreparedStatement pSt = connection.prepareStatement("update " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " set installedOn = ? where id = ? and script = ?")) {
                pSt.setString(1, now().truncatedTo(MICROS).toString());
                pSt.setString(2, TABLE_LOCKER_UUID);
                pSt.setString(3, TABLE_LOCKER_SCRIPT);
                int updateCount = pSt.executeUpdate();
                if (updateCount == 0) throw shouldNotHappenException(new IllegalStateException("Another DatabaseCreator is performing the migrations table."));
            }
        }

        private void removeLock(Connection conn) throws SQLException {
            try (final PreparedStatement pSt = conn.prepareStatement("delete from " + tablePrefixStatementUpdater.getFQTableName("jobrunr_migrations") + " where id = ?")) {
                pSt.setString(1, TABLE_LOCKER_UUID);
                int updateCount = pSt.executeUpdate();
                if (updateCount == 0) throw shouldNotHappenException(new IllegalStateException("The migrations table lock has already been removed."));
            }
            LOGGER.debug("The lock has been removed from migrations table.");
        }

        private Connection getConnection() throws SQLException {
            return connectionProvider.getConnection();
        }
    }
}
