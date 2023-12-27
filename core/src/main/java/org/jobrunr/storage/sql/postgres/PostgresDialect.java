package org.jobrunr.storage.sql.postgres;

import org.jobrunr.storage.sql.common.db.Dialect;

public class PostgresDialect implements Dialect {

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
        return " FOR UPDATE SKIP LOCKED";
    }
}
