package org.jobrunr.storage.sql.common.db;

public class AnsiDialect implements Dialect {

    @Override
    public String limit() {
        return "LIMIT :limit";
    }

    @Override
    public String limitAndOffset() {
        return "LIMIT :limit OFFSET :offset";
    }
}