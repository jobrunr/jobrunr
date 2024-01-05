package org.assertj.db;

public class MariaDBDatabaseQueries extends DefaultDatabaseQueries {

    @Override
    public String getAllViewsQuery() {
        return "SELECT CONCAT(CONCAT(table_schema, '.'), table_name) as view_name FROM INFORMATION_SCHEMA.TABLES";
    }

    @Override
    public String getAllIndicesQuery() {
        return "select index_name\n" +
                "from information_schema.statistics\n" +
                "where table_schema not in ('information_schema', 'mysql', 'performance_schema', 'sys')";
    }
}
