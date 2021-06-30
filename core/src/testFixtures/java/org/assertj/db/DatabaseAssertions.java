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
    private List<String> views;

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

    public DatabaseAssertions hasView(String viewName) {
        Assertions.assertThat(allViews()).anyMatch(someTableName -> someTableName.toUpperCase().contains(viewName.toUpperCase()));
        return this;
    }

    public DatabaseAssertions hasView(String schema, String viewName) {
        Assertions.assertThat(allViews()).contains((schema + "." + viewName).toUpperCase());
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
                final DatabaseQueries databaseQueries = getDatabaseQueries(connection);

                try (ResultSet rs = statement.executeQuery(databaseQueries.getAllTablesQuery())) {
                    tablenames = new ArrayList<>();
                    while (rs.next()) {
                        String tableSchema = rs.getString("table_schema");
                        String tableName = rs.getString("table_name");

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
                final DatabaseQueries databaseQueries = getDatabaseQueries(connection);
                try (ResultSet rs = statement.executeQuery(databaseQueries.getAllIndicesQuery())) {
                    indices = new ArrayList<>();
                    while (rs.next()) {
                        indices.add(rs.getString("index_name").toUpperCase());
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return indices;
    }

    private List<String> allViews() {
        if (views == null) {
            try (final Connection connection = actual.getConnection(); final Statement statement = connection.createStatement()) {
                final DatabaseQueries databaseQueries = getDatabaseQueries(connection);
                try (ResultSet rs = statement.executeQuery(databaseQueries.getAllViewsQuery())) {
                    views = new ArrayList<>();
                    while (rs.next()) {
                        views.add(rs.getString("view_name").toUpperCase());
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return views;
    }

    private DatabaseQueries getDatabaseQueries(Connection connection) throws SQLException {
        String databaseName = connection.getMetaData().getDatabaseProductName();
        return DatabaseQueriesByProvider.getFor(databaseName);
    }
}
