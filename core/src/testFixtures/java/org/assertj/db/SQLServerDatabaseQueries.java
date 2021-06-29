package org.assertj.db;

public class SQLServerDatabaseQueries extends DefaultDatabaseQueries {

    @Override
    public String getAllIndicesQuery() {
        return "SELECT idx.name as index_name FROM sys.indexes idx WHERE idx.name IS NOT NULL";
    }
}
