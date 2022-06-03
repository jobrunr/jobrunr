package org.jobrunr.storage.sql.common.tables;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.utils.StringUtils.substringAfterLast;

public class AnsiDatabaseTablePrefixStatementUpdater implements TablePrefixStatementUpdater {

    private final String tablePrefix;
    private final String indexPrefix;

    public AnsiDatabaseTablePrefixStatementUpdater(String tablePrefix) {
        this.tablePrefix = tablePrefix;
        this.indexPrefix = getIndexPrefix(tablePrefix);
    }

    @Override
    public String updateStatement(String statement) {
        if (isCreateIndex(statement)) {
            return updateStatementWithTablePrefixForCreateIndexStatement(statement);
        }
        return updateStatementWithTablePrefixForOtherStatements(statement);
    }

    @Override
    public String getFQTableName(String tableName) {
        return elementPrefixer(tablePrefix, tableName);
    }

    private boolean isCreateIndex(String statement) {
        return statement.contains("CREATE INDEX ");
    }

    private String updateStatementWithTablePrefixForCreateIndexStatement(String statement) {
        return statement
                .replace("CREATE INDEX jobrunr_", "CREATE INDEX " + elementPrefixer(indexPrefix, DEFAULT_PREFIX))
                .replace("ON jobrunr_", "ON " + elementPrefixer(tablePrefix, DEFAULT_PREFIX));
    }

    private String updateStatementWithTablePrefixForOtherStatements(String statement) {
        return statement.replace(DEFAULT_PREFIX, elementPrefixer(tablePrefix, DEFAULT_PREFIX));
    }

    private String getIndexPrefix(String tablePrefix) {
        return substringAfterLast(tablePrefix, ".");
    }
}
