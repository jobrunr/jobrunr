package org.jobrunr.storage.sql;

import org.jobrunr.storage.sql.common.db.Transaction;
import org.jobrunr.utils.exceptions.Exceptions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.function.Function;

public class DatabaseCleaner {

    private final DataSource dataSource;
    private final Function<Exception, Boolean> canIgnoreException;

    public DatabaseCleaner(DataSource dataSource) {
        this(dataSource, null);
    }

    public DatabaseCleaner(DataSource dataSource, Function<Exception, Boolean> canIgnoreException) {
        this.dataSource = dataSource;
        this.canIgnoreException = canIgnoreException;
    }

    public void dropAllTablesAndViews() {
        drop("view jobrunr_jobs_stats");
        drop("table jobrunr_recurring_jobs");
        drop("table jobrunr_job_counters");
        drop("table jobrunr_jobs");
        drop("table jobrunr_backgroundjobservers");
        drop("table jobrunr_metadata");
        drop("table jobrunr_migrations");
    }

    public void deleteAllDataInTables() {
        delete("from jobrunr_recurring_jobs");
        delete("from jobrunr_job_counters");
        delete("from jobrunr_jobs");
        delete("from jobrunr_backgroundjobservers");
        delete("from jobrunr_metadata");
        insertInitialData();
    }

    private void delete(String name) {
        doInTransaction(statement -> statement.executeUpdate("delete " + name), "Error deleting from " + name);
    }

    private void drop(String name) {
        doInTransaction(statement -> statement.executeUpdate("drop " + name), "Error dropping " + name);
    }

    private void insertInitialData() {
        doInTransaction(statement -> statement
                        .executeUpdate("insert into jobrunr_metadata values ('succeeded-jobs-counter-cluster', 'succeeded-jobs-counter', 'cluster', '0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"),
                "Error inserting initial data");
    }

    private void doInTransaction(Exceptions.ThrowingConsumer<Statement> inTransaction, String errorMsg) {
        try (final Connection connection = dataSource.getConnection();
             final Transaction tran = new Transaction(connection);
             final Statement statement = connection.createStatement()) {
            inTransaction.accept(statement);
            tran.commit();
        } catch (Exception e) {
            if (canNotIgnoreException(e)) {
                System.out.println(errorMsg);
                e.printStackTrace();
            }
        }
    }

    private boolean canNotIgnoreException(Exception e) {
        return !canIgnoreException(e);
    }

    protected boolean canIgnoreException(Exception e) {
        if (this.canIgnoreException != null) {
            return this.canIgnoreException.apply(e);
        }
        return true;
    }
}
