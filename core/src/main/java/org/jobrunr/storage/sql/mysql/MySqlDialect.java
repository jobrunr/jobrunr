package org.jobrunr.storage.sql.mysql;

import org.jobrunr.storage.sql.common.db.AnsiDialect;

import static org.jobrunr.utils.VersionNumber.v;

public class MySqlDialect extends AnsiDialect {

    private final boolean supportsSelectForUpdateSkipLocked;

    public MySqlDialect(String databaseName, String databaseVersion) {
        this.supportsSelectForUpdateSkipLocked = isMySQL("8.0.1", databaseName, databaseVersion);
    }

    @Override
    public String selectForUpdateSkipLocked() {
        return supportsSelectForUpdateSkipLocked ? " FOR UPDATE SKIP LOCKED" : "";
    }

    private boolean isMySQL(String expectedVersion, String databaseName, String databaseVersion) {
        return databaseName.equalsIgnoreCase("MySQL") && v(databaseVersion).hasMajorMinorAndPatchVersionHigherOrEqualTo(expectedVersion);
    }
}
