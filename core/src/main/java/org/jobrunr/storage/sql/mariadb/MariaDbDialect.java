package org.jobrunr.storage.sql.mariadb;

import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.utils.VersionNumber;

public class MariaDbDialect implements Dialect {

    private final boolean supportsSelectForUpdateSkipLocked;

    public MariaDbDialect(String databaseName, String databaseVersion, String databaseDriverVersion) {
        this.supportsSelectForUpdateSkipLocked = isMariaDb("10.6", databaseName, databaseVersion);
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

    private boolean isMariaDb(String expectedVersion, String databaseName, String databaseVersion) {
        return databaseName.equalsIgnoreCase("MariaDB") && VersionNumber.isNewerOrEqualTo(databaseVersion, expectedVersion);
    }
}
