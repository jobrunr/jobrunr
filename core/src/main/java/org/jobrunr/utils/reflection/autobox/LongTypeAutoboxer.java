package org.jobrunr.utils.reflection.autobox;

import java.math.BigDecimal;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class LongTypeAutoboxer implements TypeAutoboxer<Long> {
    @Override
    public boolean supports(Class<?> type) {
        return long.class.equals(type) || Long.class.equals(type);
    }

    @Override
    public Long autobox(Object value, Class<Long> type) {
        if (value instanceof Long) {
            return cast(value);
        } else if (value instanceof BigDecimal) {
            return cast(((BigDecimal) value).longValue());
        } else if (value instanceof Integer) {
            return cast(Long.valueOf((Integer) value));
        } else if (value instanceof String) {
            return cast(Long.valueOf((String) value));
        }
        throw new UnsupportedOperationException(String.format("Cannot autobox %s of type %s to %s", value, value.getClass().getName(), Long.class.getName()));
    }
}
