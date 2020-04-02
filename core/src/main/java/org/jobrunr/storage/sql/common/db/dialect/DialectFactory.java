package org.jobrunr.storage.sql.common.db.dialect;

import javax.sql.DataSource;

public class DialectFactory {

    private DialectFactory() {
    }

    private static final OracleDialect oracleDialect = new OracleDialect();
    private static final AnsiDialect ansiDialect = new AnsiDialect();

    public static Dialect forDataSource(DataSource dataSource) {
        if (dataSource.getClass().getPackage().getName().contains("oracle")) {
            return oracleDialect;
        }
        return ansiDialect;
    }

}
