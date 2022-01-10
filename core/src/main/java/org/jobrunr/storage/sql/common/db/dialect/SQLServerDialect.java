package org.jobrunr.storage.sql.common.db.dialect;

public class SQLServerDialect implements Dialect {

    @Override
    public String limitAndOffset(String order) {
        return " ORDER BY " + order + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
    }

    @Override
    public String escape(String toEscape) {
        return toEscape;
    }
}
