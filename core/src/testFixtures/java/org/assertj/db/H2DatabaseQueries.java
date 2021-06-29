package org.assertj.db;

public class H2DatabaseQueries extends DefaultDatabaseQueries {

    @Override
    public String getAllIndicesQuery() {
        return "SELECT distinct index_name as index_name FROM information_schema.indexes";
    }
}
