package org.jobrunr.storage.sql.common.db;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.jobrunr.utils.reflection.ReflectionUtils.autobox;

public class SqlResultSet {

    private final Set<String> columns;
    private final Map<String, Object> values;

    public SqlResultSet(ResultSet rs) throws SQLException {
        this.columns = getColumns(rs);
        this.values = initValues(rs, columns);
    }

    private Map<String, Object> initValues(ResultSet rs, Set<String> columns) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        for (String columnName : columns) {
            result.put(columnName, rs.getObject(columnName));
        }
        return result;
    }

    private Object val(String name) {
        return values.get(name.toLowerCase());
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
        return autobox(val(name), Instant.class);
    }

    public Duration asDuration(String name) {
        return autobox(val(name), Duration.class);
    }

    public float asFloat(String name) {
        return autobox(val(name), Float.class);
    }

    public double asDouble(String name) {
        return autobox(val(name), Double.class);
    }

    public BigDecimal asBigDecimal(String name) {
        return autobox(val(name), BigDecimal.class);
    }

    private Set<String> getColumns(ResultSet resultSet) throws SQLException {
        Set<String> result = new HashSet<>();
        final ResultSetMetaData metaData = resultSet.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            result.add(metaData.getColumnName(i).toLowerCase());
        }
        return result;
    }
}
