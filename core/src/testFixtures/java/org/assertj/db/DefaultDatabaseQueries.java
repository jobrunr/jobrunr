package org.assertj.db;

public class DefaultDatabaseQueries implements DatabaseQueries {

    @Override
    public String getAllTablesQuery() {
        return "SELECT * FROM INFORMATION_SCHEMA.TABLES";
    }

    @Override
    public String getAllIndicesQuery() {
        return "SELECT distinct index_name as index_name FROM information_schema.indexes";
    }
}
