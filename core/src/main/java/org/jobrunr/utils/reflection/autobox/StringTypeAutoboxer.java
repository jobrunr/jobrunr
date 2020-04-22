package org.jobrunr.utils.reflection.autobox;

import java.sql.Clob;
import java.sql.SQLException;

import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class StringTypeAutoboxer implements TypeAutoboxer<String> {
    @Override
    public boolean supports(Class<?> type) {
        return String.class.equals(type);
    }

    @Override
    public String autobox(Object value, Class<String> type) {
        if (value instanceof Clob) {
            try {
                Clob clob = (Clob) value;
                return cast(clob.getSubString(1, (int) clob.length()));
            } catch (SQLException e) {
                throw shouldNotHappenException(e);
            }
        }
        throw new UnsupportedOperationException(String.format("Cannot autobox %s of type %s to %s", value, value.getClass().getName(), String.class.getName()));
    }
}
