package org.jobrunr.storage.sql.common.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.UUID;

import static java.util.TimeZone.getTimeZone;

public interface Dialect {

    String limit();

    String limitAndOffset();

    default String escape(String toEscape) {
        return toEscape;
    }

    default void setParam(PreparedStatement ps, int i, Object o) throws SQLException {
        if (o instanceof String) {
            setString(ps, i, (String) o);
        } else if (o instanceof UUID) {
            setUUID(ps, i, (UUID) o);
        } else if (o instanceof Integer) {
            setInt(ps, i, (Integer) o);
        } else if (o instanceof Long) {
            setLong(ps, i, (Long) o);
        } else if (o instanceof Double) {
            setDouble(ps, i, (Double) o);
        } else if (o instanceof Boolean) {
            setBoolean(ps, i, (boolean) o);
        } else if (o instanceof Instant) {
            setTimestamp(ps, i, (Instant) o);
        } else if (o instanceof Duration) {
            setDuration(ps, i, (Duration) o);
        } else if (o instanceof Enum) {
            setEnum(ps, i, (Enum) o);
        } else if (o == null) {
            //TODO hack, use TypeResolver to keep track of type
            ps.setTimestamp(i, null);
        } else {
            throw new IllegalStateException(String.format("Found a value which could not be set in the PreparedStatement: %s: %s", o.getClass(), o));
        }
    }

    default void setString(PreparedStatement ps, int i, String stringValue) throws SQLException {
        ps.setString(i, stringValue);
    }

    default void setUUID(PreparedStatement ps, int i, UUID uuidValue) throws SQLException {
        ps.setString(i, uuidValue.toString());
    }

    default void setInt(PreparedStatement ps, int i, Integer intValue) throws SQLException {
        ps.setInt(i, intValue);
    }

    default void setLong(PreparedStatement ps, int i, Long longValue) throws SQLException {
        ps.setLong(i, longValue);
    }

    default void setDouble(PreparedStatement ps, int i, Double doubleValue) throws SQLException {
        ps.setDouble(i, doubleValue);
    }

    default void setBoolean(PreparedStatement ps, int i, boolean boolValue) throws SQLException {
        ps.setInt(i, boolValue ? 1 : 0);
    }

    default void setTimestamp(PreparedStatement ps, int i, Instant instant) throws SQLException {
        ps.setTimestamp(i, Timestamp.from(instant), Calendar.getInstance(getTimeZone(ZoneOffset.UTC)));
    }

    default void setDuration(PreparedStatement ps, int i, Duration duration) throws SQLException {
        ps.setString(i, duration.toString());
    }

    default void setEnum(PreparedStatement ps, int i, Enum enumValue) throws SQLException {
        ps.setString(i, ((Enum<?>) enumValue).name());
    }

}