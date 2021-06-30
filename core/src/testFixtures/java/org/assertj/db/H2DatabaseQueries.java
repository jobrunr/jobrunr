package org.assertj.db;

public class H2DatabaseQueries extends DefaultDatabaseQueries {

    @Override
    public String getAllViewsQuery() {
        return "SELECT CONCAT(CONCAT(table_schema, '.'), table_name) as view_name FROM INFORMATION_SCHEMA.TABLES";
    }

    @Override
    public String getAllIndicesQuery() {
        return "SELECT distinct index_name as index_name FROM information_schema.indexes";
    }
}
