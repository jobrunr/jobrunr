package org.jobrunr.storage.sql.common.db;

import java.sql.Connection;
import java.sql.SQLException;

public class Transaction implements AutoCloseable {

    private final Connection conn;
    private final boolean originalAutoCommit;
    private final Boolean enableAutocommit;
    private boolean committed;

    public Transaction(Connection conn) throws SQLException {
        this(conn, null);
    }

    public Transaction(Connection conn, Boolean enableAutocommit) throws SQLException {
        this.conn = conn;
        this.originalAutoCommit = conn.getAutoCommit();
        this.enableAutocommit = enableAutocommit;
        if (enableAutocommit != null) {
            conn.setAutoCommit(enableAutocommit);
        }
    }

    public void commit() throws SQLException {
        if (isAutoCommitDisabled()) {
            conn.commit();
        }
        committed = true;
    }

    @Override
    public void close() throws SQLException {
        if (!committed && isAutoCommitDisabled()) {
            conn.rollback();
        }
        // we messed with autocommit, restore original value
        if (enableAutocommit != null) {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    private boolean isAutoCommitDisabled() {
        return !originalAutoCommit || (enableAutocommit != null && !enableAutocommit);
    }
}
