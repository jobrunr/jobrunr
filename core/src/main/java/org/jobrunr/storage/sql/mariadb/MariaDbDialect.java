package org.jobrunr.storage.sql.mariadb;

import org.jobrunr.storage.sql.common.db.AnsiDialect;

import static org.jobrunr.utils.VersionNumber.v;

public class MariaDbDialect extends AnsiDialect {

    private final boolean supportsSelectForUpdateSkipLocked;

    public MariaDbDialect(String databaseName, String databaseVersion) {
        this.supportsSelectForUpdateSkipLocked = isMariaDb("10.6", databaseName, databaseVersion);
    }

    @Override
    public String selectForUpdateSkipLocked() {
        return supportsSelectForUpdateSkipLocked ? " FOR UPDATE SKIP LOCKED" : "";
    }

    private boolean isMariaDb(String expectedVersion, String databaseName, String databaseVersion) {
        return databaseName.equalsIgnoreCase("MariaDB") && v(databaseVersion).hasMajorAndMinorVersionHigherOrEqualTo(expectedVersion);
    }
}
