package org.assertj.db;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class DatabaseAssertions extends AbstractAssert<DatabaseAssertions, DataSource> {

    private List<String> tablenames;
    private List<String> indices;

    private DatabaseAssertions(DataSource dataSource) {
        super(dataSource, DatabaseAssertions.class);
    }

    public static DatabaseAssertions assertThat(DataSource dataSource) {
        return new DatabaseAssertions(dataSource);
    }

    public DatabaseAssertions hasTable(String tableName) {
        Assertions.assertThat(allTables()).anyMatch(someTableName -> someTableName.toUpperCase().contains(tableName.toUpperCase()));
        return this;
    }

    public DatabaseAssertions hasTable(String schema, String tableName) {
        Assertions.assertThat(allTables()).contains((schema + "." + tableName).toUpperCase());
        return this;
    }

    public DatabaseAssertions hasView(String tableName) {
        Assertions.assertThat(allTables()).anyMatch(someTableName -> someTableName.toUpperCase().contains(tableName.toUpperCase()));
        return this;
    }

    public DatabaseAssertions hasView(String schema, String tableName) {
        Assertions.assertThat(allTables()).contains((schema + "." + tableName).toUpperCase());
        return this;
    }

    public DatabaseAssertions hasIndex(String schema, String tableName) {
        Assertions.assertThat(allIndices()).contains((schema + "." + tableName).toUpperCase());
        return this;
    }

    public DatabaseAssertions hasIndexesMatching(int times, Condition<String> condition) {
        Assertions.assertThat(allIndices()).areAtLeast(times, condition);
        return this;
    }

    public DatabaseAssertions hasIndex(Predicate<String> predicate) {
        Assertions.assertThat(allIndices()).anyMatch(predicate);
        return this;
    }

    private List<String> allTables() {
        if (tablenames == null) {
            try (final Connection connection = actual.getConnection(); final Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES")) {
                    tablenames = new ArrayList<>();
                    while (rs.next()) {
                        String tableCatalog = rs.getString(1);
                        String tableSchema = rs.getString(2);
                        String tableName = rs.getString(3);

                        tablenames.add((tableSchema + "." + tableName).toUpperCase());
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return tablenames;
    }

    private List<String> allIndices() {
        if (indices == null) {
            try (final Connection connection = actual.getConnection(); final Statement statement = connection.createStatement()) {
                String indexQuery = getIndexQuery(connection);
                try (ResultSet rs = statement.executeQuery(indexQuery)) {
                    indices = new ArrayList<>();
                    while (rs.next()) {
                        indices.add(rs.getString("indexname").toUpperCase());
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return indices;
    }

    private String getIndexQuery(Connection connection) throws SQLException {
        String databaseName = connection.getMetaData().getDatabaseProductName();
        switch (databaseName) {
            case "Microsoft SQL Server":
                return "SELECT idx.name as indexname FROM sys.indexes idx WHERE idx.name IS NOT NULL";
            case "Oracle":
                throw new UnsupportedOperationException("Implement me");
            case "PostgreSQL":
                return "SELECT indexname FROM pg_indexes";
            case "H2":
            case "HSQL Database Engine":
                return "SELECT distinct index_name as indexname FROM information_schema.indexes";
            case "MySQL":
            case "MariaDB":
                return "SELECT indexname FROM INFORMATION_SCHEMA.STATISTICS";
            case "SQLite":
                return "SELECT name FROM sqlite_master WHERE type = 'index'";
            case "DB2":
                throw new UnsupportedOperationException("Implement me");
            default: // SQL Standard
                throw new UnsupportedOperationException("Implement me");
        }
    }
}
