package org.jobrunr.storage.sql.common.tables;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.utils.StringUtils.substringAfterLast;

public class AnsiDatabaseTablePrefixStatementUpdater implements TablePrefixStatementUpdater {

    protected final String tablePrefix;
    protected final String indexPrefix;

    public AnsiDatabaseTablePrefixStatementUpdater(String tablePrefix) {
        this.tablePrefix = tablePrefix;
        this.indexPrefix = getIndexPrefix(tablePrefix);
    }

    @Override
    public String updateStatement(String statement) {
        if (isIndexStatement(statement)) {
            return updateStatementWithTablePrefixForIndexStatement(statement);
        }
        return updateStatementWithTablePrefixForOtherStatements(statement);
    }

    @Override
    public String getFQTableName(String tableName) {
        return elementPrefixer(tablePrefix, tableName);
    }

    private boolean isIndexStatement(String statement) {
        return statement.contains("CREATE INDEX ") || statement.contains("CREATE UNIQUE INDEX ") || statement.contains("DROP INDEX ");
    }

    protected String updateStatementWithTablePrefixForIndexStatement(String statement) {
        String updatedStatement;
        if(statement.toUpperCase().contains(" ON ")) {
            updatedStatement = statement
                    .replace("CREATE UNIQUE INDEX jobrunr_", "CREATE UNIQUE INDEX " + elementPrefixer(indexPrefix, DEFAULT_PREFIX))
                    .replace("CREATE INDEX jobrunr_", "CREATE INDEX " + elementPrefixer(indexPrefix, DEFAULT_PREFIX))
                    .replace("DROP INDEX jobrunr_", "DROP INDEX " + elementPrefixer(indexPrefix, DEFAULT_PREFIX))
                    .replace("ON jobrunr_", "ON " + elementPrefixer(tablePrefix, DEFAULT_PREFIX));
        } else {
            updatedStatement = statement
                    .replace("CREATE UNIQUE INDEX jobrunr_", "CREATE UNIQUE INDEX " + elementPrefixer(indexPrefix, DEFAULT_PREFIX))
                    .replace("CREATE INDEX jobrunr_", "CREATE INDEX " + elementPrefixer(indexPrefix, DEFAULT_PREFIX))
                    .replace("DROP INDEX jobrunr_", "DROP INDEX " + elementPrefixer(tablePrefix, DEFAULT_PREFIX));
        }
        return updatedStatement;
    }

    private String updateStatementWithTablePrefixForOtherStatements(String statement) {
        return statement.replace(DEFAULT_PREFIX, elementPrefixer(tablePrefix, DEFAULT_PREFIX));
    }

    private String getIndexPrefix(String tablePrefix) {
        return substringAfterLast(tablePrefix, ".");
    }
}
