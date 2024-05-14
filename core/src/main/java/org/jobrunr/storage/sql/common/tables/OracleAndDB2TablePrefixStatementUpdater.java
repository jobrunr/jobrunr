package org.jobrunr.storage.sql.common.tables;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.utils.StringUtils.substringBefore;

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
    public String getSchema() {
        if (tablePrefix.contains(".")) {
            return substringBefore(tablePrefix, ".");
        }
        return null;
    }

    @Override
    public String getFQTableName(String tableName) {
        return elementPrefixer(tablePrefix, tableName);
    }
}