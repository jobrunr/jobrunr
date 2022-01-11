package org.jobrunr.storage.sql.common.db.dialect;

public class AnsiDialect implements Dialect {

    @Override
    public String limitAndOffset(String order) {
        return " ORDER BY " + order + " LIMIT :limit OFFSET :offset";
    }

    @Override
    public String escape(String toEscape) {
        return toEscape;
    }
}
