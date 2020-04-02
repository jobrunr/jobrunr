package org.jobrunr.storage.sql.common.db.dialect;

public class AnsiDialect implements Dialect {
    @Override
    public String limitAndOffset() {
        return " LIMIT :limit OFFSET :offset";
    }
}
