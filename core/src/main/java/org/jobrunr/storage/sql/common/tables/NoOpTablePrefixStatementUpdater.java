package org.jobrunr.storage.sql.common.tables;

public class NoOpTablePrefixStatementUpdater implements TablePrefixStatementUpdater {

    @Override
    public String updateStatement(String statement) {
        return statement;
    }

    @Override
    public String getFQTableName(String tableName) {
        return tableName;
    }

}
