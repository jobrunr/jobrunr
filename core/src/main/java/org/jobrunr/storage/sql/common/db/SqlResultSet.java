package org.jobrunr.storage.sql.common.db;

import org.jobrunr.storage.StorageException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static java.util.TimeZone.getTimeZone;
import static org.jobrunr.utils.reflection.ReflectionUtils.autobox;

public class SqlResultSet {

    private final List<String> columns;
    private final ResultSet rs;

    public SqlResultSet(List<String> columns, ResultSet rs) {
        this.columns = columns;
        this.rs = rs;
    }

    public boolean asBoolean(String name) {
        return autobox(val(name), Boolean.class);
    }

    public String asString(String name) {
        return autobox(val(name), String.class);
    }

    public int asInt(String name) {
        return autobox(val(name), Integer.class);
    }

    public UUID asUUID(String name) {
        return autobox(val(name), UUID.class);
    }

    public long asLong(String name) {
        return autobox(val(name), Long.class);
    }

    public Instant asInstant(String name) {
        return autobox(valAsUTCTimestamp(name), Instant.class);
    }

    public Duration asDuration(String name) {
        return autobox(val(name), Duration.class);
    }

    public double asDouble(String name) {
        return autobox(val(name), Double.class);
    }

    private Object val(String name) {
        try {
            return rs.getObject(columns.indexOf(name.toLowerCase()));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    private Timestamp valAsUTCTimestamp(String name) {
        try {
            return rs.getTimestamp(columns.indexOf(name.toLowerCase()), Calendar.getInstance(getTimeZone(ZoneOffset.UTC)));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }
}
