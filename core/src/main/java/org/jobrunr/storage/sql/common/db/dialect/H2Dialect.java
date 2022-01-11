package org.jobrunr.storage.sql.common.db.dialect;

public class H2Dialect extends AnsiDialect {

    @Override
    public String escape(String toEscape) {
        return toEscape.replaceAll("(?<!\\S)value(?!\\S)", "`value`");
    }
}
