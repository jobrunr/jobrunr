package org.assertj.db;

public class PostgresDatabaseQueries extends DefaultDatabaseQueries {

    @Override
    public String getAllViewsQuery() {
        return "SELECT CONCAT(CONCAT(table_schema, '.'), table_name) as view_name from INFORMATION_SCHEMA.views;";
    }

    @Override
    public String getAllIndicesQuery() {
        return "SELECT indexname as index_name FROM pg_indexes";
    }
}
