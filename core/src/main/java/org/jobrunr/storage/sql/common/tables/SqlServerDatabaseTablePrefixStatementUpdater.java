package org.jobrunr.storage.sql.common.tables;

public class SqlServerDatabaseTablePrefixStatementUpdater extends AnsiDatabaseTablePrefixStatementUpdater {

    public SqlServerDatabaseTablePrefixStatementUpdater(String tablePrefix) {
        super(tablePrefix);
    }

    protected String updateStatementWithTablePrefixForIndexStatement(String statement) {
        return statement
                .replace("CREATE UNIQUE INDEX jobrunr_", "CREATE UNIQUE INDEX " + indexPrefix + DEFAULT_PREFIX)
                .replace("CREATE INDEX jobrunr_", "CREATE INDEX " + indexPrefix + DEFAULT_PREFIX)
                .replace("DROP INDEX jobrunr_", "DROP INDEX " + indexPrefix + DEFAULT_PREFIX)
                .replace("ON jobrunr_", "ON " + tablePrefix + DEFAULT_PREFIX);
    }
}
