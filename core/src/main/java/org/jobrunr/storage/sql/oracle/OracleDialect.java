package org.jobrunr.storage.sql.oracle;

import org.jobrunr.storage.sql.common.db.Dialect;

public class OracleDialect implements Dialect {

    @Override
    public String limit() {
        return "FETCH NEXT :limit ROWS ONLY";
    }

    @Override
    public String limitAndOffset() {
        return "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
    }

}