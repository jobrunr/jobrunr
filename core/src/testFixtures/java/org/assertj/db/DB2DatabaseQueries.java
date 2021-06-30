package org.assertj.db;

public class DB2DatabaseQueries implements DatabaseQueries {

    @Override
    public String getAllTablesQuery() {
        return "SELECT TABSCHEMA as table_schema, TABNAME as table_name FROM SYSCAT.TABLES WHERE TYPE = 'T'";
    }

    @Override
    public String getAllViewsQuery() {
        return "select CONCAT(CONCAT(viewschema, '.'), viewname) as view_name from syscat.views";
    }

    @Override
    public String getAllIndicesQuery() {
        return "select CONCAT(CONCAT(INDSCHEMA, '.'), INDNAME) as index_name from syscat.indexes";
    }
}
