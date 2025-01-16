package org.jobrunr.storage.sql.postgres;

import org.jobrunr.storage.sql.common.db.AnsiDialect;

import static org.jobrunr.storage.sql.common.tables.TablePrefixStatementUpdater.DEFAULT_PREFIX;

public class PostgresDialect extends AnsiDialect {
    @Override
    public String selectForUpdateSkipLocked() {
        return " FOR UPDATE SKIP LOCKED";
    }

    @Override
    public String escape(String toEscape) {
        return toEscape.replaceAll(DEFAULT_PREFIX + "[a-z_]+", "\"$0\"");
    }
}
