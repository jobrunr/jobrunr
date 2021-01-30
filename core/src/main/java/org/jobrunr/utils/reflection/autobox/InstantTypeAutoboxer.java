package org.jobrunr.utils.reflection.autobox;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

import static java.time.ZoneId.systemDefault;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class InstantTypeAutoboxer implements TypeAutoboxer<Instant> {
    @Override
    public boolean supports(Class<?> type) {
        return Instant.class.equals(type);
    }

    @Override
    public Instant autobox(Object value, Class<Instant> type) {
        if (value instanceof Timestamp) {
            return cast(((Timestamp) value).toInstant());
        } else if (value instanceof Long) {
            return cast(new Timestamp((Long) value).toInstant());
        } else if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).atZone(systemDefault()).toInstant();
        } else if ("oracle.sql.TIMESTAMP".equals(value.getClass().getName())) {
            return new InstantForOracleTypeAutoboxer().autobox(value, type);
        } else if (value instanceof CharSequence) {
            return Instant.parse((CharSequence) value);
        }
        throw new UnsupportedOperationException(String.format("Cannot autobox %s of type %s to %s", value, value.getClass().getName(), Instant.class.getName()));
    }
}
