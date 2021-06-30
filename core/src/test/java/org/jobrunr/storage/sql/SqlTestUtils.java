package org.jobrunr.storage.sql;

import org.jobrunr.storage.sql.common.db.Transaction;
import org.jobrunr.utils.exceptions.Exceptions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.function.Function;

public class SqlTestUtils {

    private SqlTestUtils() {

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
             final Transaction tran = new Transaction(connection, false);
             final Statement statement = connection.createStatement()) {
            inTransaction.accept(statement);
            tran.commit();
        } catch (Exception e) {
            throw e;
        }
    }
}
