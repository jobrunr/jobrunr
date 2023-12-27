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

    @Override
    public String selectForUpdateSkipLocked() {
        // although it appears that Oracle supports select for update skip locked, there are problems with it
        // see: https://stackoverflow.com/questions/6117254/force-oracle-to-return-top-n-rows-with-skip-locked
        return "";
    }

}