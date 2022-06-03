package org.jobrunr.storage.sql.common.tables;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class OracleAndDB2TablePrefixStatementUpdater implements TablePrefixStatementUpdater {

    private final String tablePrefix;

    public OracleAndDB2TablePrefixStatementUpdater(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    @Override
    public String updateStatement(String statement) {
        return statement.replace(DEFAULT_PREFIX, elementPrefixer(tablePrefix, DEFAULT_PREFIX));
    }

    @Override
    public String getFQTableName(String tableName) {
        return elementPrefixer(tablePrefix, tableName);
    }
}