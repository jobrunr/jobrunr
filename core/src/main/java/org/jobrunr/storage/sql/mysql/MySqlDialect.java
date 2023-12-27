package org.jobrunr.storage.sql.mysql;

import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.utils.VersionNumber;

public class MySqlDialect implements Dialect {

    private final boolean supportsSelectForUpdateSkipLocked;

    public MySqlDialect(String databaseName, String databaseVersion) {
        this.supportsSelectForUpdateSkipLocked = isMySQL("8.0.1", databaseName, databaseVersion);
    }

    @Override
    public String limit() {
        return "LIMIT :limit";
    }

    @Override
    public String limitAndOffset() {
        return "LIMIT :limit OFFSET :offset";
    }

    @Override
    public String selectForUpdateSkipLocked() {
        return supportsSelectForUpdateSkipLocked ? " FOR UPDATE SKIP LOCKED" : "";
    }

    private boolean isMySQL(String expectedVersion, String databaseName, String databaseVersion) {
        return databaseName.equalsIgnoreCase("MySQL") && VersionNumber.isNewerOrEqualTo(databaseVersion, expectedVersion);
    }
}
