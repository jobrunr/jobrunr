package org.jobrunr.storage.sql.common.db;

import java.sql.Connection;
import java.sql.SQLException;

public class Transaction implements AutoCloseable {

    private final Connection conn;
    private final boolean originalAutoCommit;
    private boolean committed;

    public Transaction(Connection conn) throws SQLException {
        this.conn = conn;
        this.originalAutoCommit = conn.getAutoCommit();
        if (originalAutoCommit) {
            conn.setAutoCommit(false);
        }
    }

    public void commit() throws SQLException {
        conn.commit();
        committed = true;
    }

    @Override
    public void close() throws SQLException {
        if (!committed) {
            conn.rollback();
        }
        // we messed with autocommit, restore original value
        if (originalAutoCommit != conn.getAutoCommit()) {
            conn.setAutoCommit(originalAutoCommit);
        }
    }
}
