package org.jobrunr.storage.sql.common.db.dialect;

public class OracleDialect implements Dialect {
    @Override
    public String limitAndOffset() {
        return " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
    }
}
