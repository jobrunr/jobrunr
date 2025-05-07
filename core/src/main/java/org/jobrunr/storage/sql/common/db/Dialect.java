package org.jobrunr.storage.sql.common.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static java.util.TimeZone.getTimeZone;

public interface Dialect {

    String limit();

    String limitAndOffset();

    default String selectForUpdateSkipLocked() {
        return "";
    }

    default String escape(String toEscape) {
        return toEscape;
    }

    default void setParam(PreparedStatement ps, int i, String paramName, Object o) throws SQLException {
        if (o instanceof String) {
            setString(ps, i, paramName, (String) o);
        } else if (o instanceof UUID) {
            setUUID(ps, i, paramName, (UUID) o);
        } else if (o instanceof Integer) {
            setInt(ps, i, paramName, (Integer) o);
        } else if (o instanceof Long) {
            setLong(ps, i, paramName, (Long) o);
        } else if (o instanceof Double) {
            setDouble(ps, i, paramName, (Double) o);
        } else if (o instanceof Boolean) {
            setBoolean(ps, i, paramName, (boolean) o);
        } else if (o instanceof Instant) {
            setTimestamp(ps, i, paramName, (Instant) o);
        } else if (o instanceof Duration) {
            setDuration(ps, i, paramName, (Duration) o);
        } else if (o instanceof Enum) {
            setEnum(ps, i, paramName, (Enum) o);
        } else if (o instanceof List<?>) {
            List<?> list = (List<?>) o;
            setParam(ps, i, paramName, list.remove(0));
        } else if (o == null) {
            setNull(ps, i, paramName);
        } else {
            throw new IllegalStateException(String.format("Found a value which could not be set in the preparedstatement: %s: %s", o.getClass(), o));
        }
    }

    default void setString(PreparedStatement ps, int i, String paramName, String stringValue) throws SQLException {
        ps.setString(i, stringValue);
    }

    default void setUUID(PreparedStatement ps, int i, String paramName, UUID uuidValue) throws SQLException {
        ps.setString(i, uuidValue.toString());
    }

    default void setInt(PreparedStatement ps, int i, String paramName, Integer intValue) throws SQLException {
        ps.setInt(i, intValue);
    }

    default void setLong(PreparedStatement ps, int i, String paramName, Long longValue) throws SQLException {
        ps.setLong(i, longValue);
    }

    default void setDouble(PreparedStatement ps, int i, String paramName, Double doubleValue) throws SQLException {
        ps.setDouble(i, doubleValue);
    }

    default void setBoolean(PreparedStatement ps, int i, String paramName, boolean boolValue) throws SQLException {
        ps.setInt(i, boolValue ? 1 : 0);
    }

    default void setTimestamp(PreparedStatement ps, int i, String paramName, Instant instant) throws SQLException {
        ps.setTimestamp(i, Timestamp.from(instant), Calendar.getInstance(getTimeZone(ZoneOffset.UTC)));
    }

    default void setDuration(PreparedStatement ps, int i, String paramName, Duration duration) throws SQLException {
        ps.setString(i, duration.toString());
    }

    default void setEnum(PreparedStatement ps, int i, String paramName, Enum enumValue) throws SQLException {
        ps.setString(i, ((Enum<?>) enumValue).name());
    }

    default void setNull(PreparedStatement ps, int i, String paramName) throws SQLException {
        ps.setNull(i, Types.NULL);
    }

    default boolean isUniqueConstraintException(SQLException e) {
        String lowerCaseMessage = e.getMessage().toLowerCase();
        return e.getErrorCode() == -803 || lowerCaseMessage.contains("duplicate") || lowerCaseMessage.contains("primary key") || lowerCaseMessage.contains("unique constraint");
    }

}