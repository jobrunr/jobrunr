package org.jobrunr.storage.sql.h2;

import org.jobrunr.storage.sql.common.db.AnsiDialect;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class H2Dialect extends AnsiDialect {

    @Override
    public String escape(String toEscape) {
        return toEscape.replaceAll("(?<=[.\\s])value(?!\\S)", "`value`");
    }

    @Override
    public void setTimestamp(PreparedStatement ps, int i, String paramName, Instant instant) throws SQLException {
        // why: https://github.com/h2database/h2database/issues/3935
        Instant roundedInstant = instant.plusNanos(500).truncatedTo(ChronoUnit.MICROS);
        super.setTimestamp(ps, i, paramName, roundedInstant);
    }
}