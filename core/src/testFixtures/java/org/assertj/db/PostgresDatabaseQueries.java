package org.assertj.db;

public class PostgresDatabaseQueries extends DefaultDatabaseQueries {

    @Override
    public String getAllIndicesQuery() {
        return "SELECT indexname as index_name FROM pg_indexes";
    }
}
