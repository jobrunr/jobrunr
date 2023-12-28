package org.jobrunr.storage.sql.postgres;

import org.jobrunr.storage.sql.common.db.AnsiDialect;

public class PostgresDialect extends AnsiDialect {

    @Override
    public String selectForUpdateSkipLocked() {
        return " FOR UPDATE SKIP LOCKED";
    }
}
