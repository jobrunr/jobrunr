package org.jobrunr.storage.sql.common.db.dialect;

public class AnsiDialect implements Dialect {
    @Override
    public String limitAndOffset(String orderField, String order) {
        return " ORDER BY " + orderField + " " + order + " LIMIT :limit OFFSET :offset";
    }
}
