package org.assertj.db;

public class OracleDatabaseQueries extends DefaultDatabaseQueries {

    @Override
    public String getAllTablesQuery() {
        return "SELECT table_name, owner as table_schema FROM user_tables";
    }

    @Override
    public String getAllIndicesQuery() {
        return "select index_name from sys.all_indexes";
    }
}
