package org.jobrunr.storage.sql.common.tables;

import org.jspecify.annotations.Nullable;

public class NoOpTablePrefixStatementUpdater implements TablePrefixStatementUpdater {

    @Override
    public String updateStatement(String statement) {
        return statement;
    }

    @Override
    public @Nullable String getSchema() {
        return null;
    }

    @Override
    public String getFQTableName(String tableName) {
        return tableName;
    }

}
