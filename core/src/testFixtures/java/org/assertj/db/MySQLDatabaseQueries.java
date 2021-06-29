package org.assertj.db;

public class MySQLDatabaseQueries extends DefaultDatabaseQueries {

    @Override
    public String getAllIndicesQuery() {
        return "SELECT indexname as index_name FROM INFORMATION_SCHEMA.STATISTICS";
    }
}
