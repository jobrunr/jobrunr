package org.assertj.db;

public class SQLServerDatabaseQueries extends DefaultDatabaseQueries {

    @Override
    public String getAllViewsQuery() {
        return "SELECT CONCAT(CONCAT(table_schema, '.'), table_name) as view_name FROM INFORMATION_SCHEMA.VIEWS";
    }

    @Override
    public String getAllIndicesQuery() {
        return "SELECT idx.name as index_name FROM sys.indexes idx WHERE idx.name IS NOT NULL";
    }
}
