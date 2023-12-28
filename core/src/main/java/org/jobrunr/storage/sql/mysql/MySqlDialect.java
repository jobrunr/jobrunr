package org.jobrunr.storage.sql.mysql;

import org.jobrunr.storage.sql.common.db.AnsiDialect;
import org.jobrunr.utils.VersionNumber;

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
        return databaseName.equalsIgnoreCase("MySQL") && VersionNumber.isNewerOrEqualTo(databaseVersion, expectedVersion);
    }
}
