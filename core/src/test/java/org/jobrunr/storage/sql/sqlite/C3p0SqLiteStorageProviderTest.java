package org.jobrunr.storage.sql.sqlite;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class C3p0SqLiteStorageProviderTest extends SqlStorageProviderTest {

    private static ComboPooledDataSource dataSource;

    /**
     * Strange bug when combining c3p0 and Sqlite where although drop table succeeds, the creation of table fails because it is still there.
     */
    @Override
    protected void cleanupDatabase(DataSource dataSource) {
        boolean stillHasTables = true;
        while (stillHasTables) {
            super.cleanupDatabase(dataSource);

            try (Connection connection = dataSource.getConnection()) {
                final Statement statement = connection.createStatement();
                final ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table';");
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    if (count > 1) {
                        System.err.println(String.format("Still found %d tables.", count));
                    } else {
                        System.err.println("No tables found - finally");
                        stillHasTables = false;
                    }
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new ComboPooledDataSource();
            dataSource.setJdbcUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        }
        return dataSource;
    }
}