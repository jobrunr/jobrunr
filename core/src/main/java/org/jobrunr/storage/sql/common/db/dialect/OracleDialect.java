package org.jobrunr.storage.sql.common.db.dialect;

public class OracleDialect implements Dialect {
    @Override
    public String limitAndOffset(String orderField, String order) {
        return " ORDER BY " + orderField + " " + order + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
    }
}
