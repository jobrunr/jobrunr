package org.jobrunr.storage.sql.common.tables;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class PostgresDatabaseTablePrefixStatementUpdater extends AnsiDatabaseTablePrefixStatementUpdater {

    public PostgresDatabaseTablePrefixStatementUpdater(String tablePrefix) {
        super(tablePrefix);
    }

    @Override
    public String getFQTableName(String tableName) {
        return String.format("\"%s\"", super.getFQTableName(tableName));
    }

    private String replacePrefixed(String statement, String targetPrefix, String replacementPrefix) {
        return statement.replaceAll(
            "(" +  targetPrefix + ") jobrunr_([a-z_]+)",
            "$1 \"" + elementPrefixer(replacementPrefix, DEFAULT_PREFIX) + "$2\""
        );
    }

    @Override
    protected String updateStatementWithTablePrefixForIndexStatement(String statement) {
        String updatedStatement = statement;
        updatedStatement = replacePrefixed(updatedStatement, "CREATE(?: UNIQUE)? INDEX", indexPrefix);
        updatedStatement = replacePrefixed(updatedStatement, "DROP INDEX", indexPrefix);

        if (statement.toUpperCase().contains(" ON ")) {
            updatedStatement = replacePrefixed(updatedStatement, "ON", tablePrefix);
        }

        return updatedStatement;
    }

    @Override
    protected String updateStatementWithTablePrefixForOtherStatements(String statement) {
        return statement.replaceAll(
            String.format(" %s([a-z_]+)", DEFAULT_PREFIX),
            String.format(" \"%s$1\"", elementPrefixer(tablePrefix, DEFAULT_PREFIX))
        );
    }
}
