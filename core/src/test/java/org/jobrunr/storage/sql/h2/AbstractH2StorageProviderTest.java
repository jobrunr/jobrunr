package org.jobrunr.storage.sql.h2;

import org.h2.jdbc.JdbcSQLNonTransientConnectionException;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class AbstractH2StorageProviderTest extends SqlStorageProviderTest {

    protected static void shutdownDatabase(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("SHUTDOWN");
        } catch (JdbcSQLNonTransientConnectionException e) {
            //ignore if already closed
        }
    }
}
