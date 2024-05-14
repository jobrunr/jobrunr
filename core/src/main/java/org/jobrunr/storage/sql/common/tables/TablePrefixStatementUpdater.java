package org.jobrunr.storage.sql.common.tables;

public interface TablePrefixStatementUpdater {

    String DEFAULT_PREFIX = "jobrunr_";

    String updateStatement(String statement);

    String getSchema();

    String getFQTableName(String tableName);
}