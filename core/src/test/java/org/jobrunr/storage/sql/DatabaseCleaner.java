package org.jobrunr.storage.sql;

import org.jobrunr.utils.exceptions.Exceptions;

import javax.sql.DataSource;
import java.sql.Statement;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

public class DatabaseCleaner {

    private final DataSource dataSource;
    private final String tableNamePrefix;

    public DatabaseCleaner(DataSource dataSource) {
        this(dataSource, null);
    }

    public DatabaseCleaner(DataSource dataSource, String tableNamePrefix) {
        this.dataSource = dataSource;
        this.tableNamePrefix = isNotNullOrEmpty(tableNamePrefix) ? tableNamePrefix : "";
    }

    public void dropAllTablesAndViews() {
        drop("view " + tableNamePrefix + "jobrunr_jobs_stats");
        drop("table " + tableNamePrefix + "jobrunr_recurring_jobs");
        drop("table " + tableNamePrefix + "jobrunr_job_counters");
        drop("table " + tableNamePrefix + "jobrunr_jobs");
        drop("table " + tableNamePrefix + "jobrunr_backgroundjobservers");
        drop("table " + tableNamePrefix + "jobrunr_metadata");
        drop("table " + tableNamePrefix + "jobrunr_migrations");
    }

    public void deleteAllDataInTables() {
        delete("from " + tableNamePrefix + "jobrunr_recurring_jobs");
        delete("from " + tableNamePrefix + "jobrunr_job_counters");
        delete("from " + tableNamePrefix + "jobrunr_jobs");
        delete("from " + tableNamePrefix + "jobrunr_backgroundjobservers");
        delete("from " + tableNamePrefix + "jobrunr_metadata");
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
                        .executeUpdate("insert into " + tableNamePrefix + "jobrunr_metadata values ('succeeded-jobs-counter-cluster', 'succeeded-jobs-counter', 'cluster', '0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"),
                "Error inserting initial data");
    }

    private void doInTransaction(Exceptions.ThrowingConsumer<Statement> inTransaction, String errorMsg) {
        try {
            SqlTestUtils.doInTransaction(dataSource, inTransaction);
        } catch (Exception e) {
            System.out.println(errorMsg);
            e.printStackTrace();
        }
    }
}
