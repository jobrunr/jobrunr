package org.jobrunr.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.storage.sql.common.db.Transaction;
import org.jobrunr.utils.exceptions.Exceptions;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.function.Function;

public class SqlTestUtils {

    private SqlTestUtils() {
    }

    public static HikariDataSource toHikariDataSource(JdbcDatabaseContainer dbContainer) {
        return toHikariDataSource(dbContainer.getJdbcUrl(), dbContainer.getUsername(), dbContainer.getPassword());
    }

    public static HikariDataSource toHikariDataSource(JdbcDatabaseContainer dbContainer, String jdbcUrlSuffix) {
        return toHikariDataSource(dbContainer.getJdbcUrl() + jdbcUrlSuffix, dbContainer.getUsername(), dbContainer.getPassword());
    }

    public static HikariDataSource toHikariDataSource(String jdbcUrl, String userName, String password) {
        return toHikariDataSource(jdbcUrl, userName, password, true);
    }

    public static HikariDataSource toHikariDataSource(String jdbcUrl, String userName, String password, boolean autoCommit) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(userName);
        config.setPassword(password);
        config.setAutoCommit(autoCommit);
        return new HikariDataSource(config);
    }

    public static void deleteFile(String file) {
        try {
            Files.delete(Path.of(file));
        } catch (IOException e) {
            // nothing to do
        }
    }

    public static void doInTransaction(DataSource dataSource, Exceptions.ThrowingConsumer<Statement> inTransaction, Function<Exception, Boolean> canIgnoreException) throws Exception {
        try {
            doInTransaction(dataSource, inTransaction);
        } catch (Exception e) {
            if (canIgnoreException.apply(e)) return;
            throw e;
        }
    }

    public static void doInTransaction(DataSource dataSource, Exceptions.ThrowingConsumer<Statement> inTransaction, String errorMessage) {
        try {
            doInTransaction(dataSource, inTransaction);
        } catch (Exception e) {
            System.err.println(errorMessage);
            e.printStackTrace();
        }
    }

    public static void doInTransaction(DataSource dataSource, Exceptions.ThrowingConsumer<Statement> inTransaction) throws Exception {
        try (final Connection connection = dataSource.getConnection();
             final Transaction tran = new Transaction(connection);
             final Statement statement = connection.createStatement()) {
            inTransaction.accept(statement);
            tran.commit();
        } catch (Exception e) {
            throw e;
        }
    }
}
