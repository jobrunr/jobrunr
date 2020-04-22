package org.jobrunr.utils.reflection.autobox;

import java.math.BigDecimal;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class IntegerTypeAutoboxer implements TypeAutoboxer<Integer> {
    @Override
    public boolean supports(Class<?> type) {
        return int.class.equals(type) || Integer.class.equals(type);
    }

    @Override
    public Integer autobox(Object value, Class<Integer> type) {
        if (value instanceof Integer) {
            return cast(value);
        } else if (value instanceof BigDecimal) {
            return cast(((BigDecimal) value).intValue());
        } else if (value instanceof String) {
            return cast(Integer.valueOf((String) value));
        } else if (value instanceof Long) {
            return cast(((Long) value).intValue());
        }
        throw new UnsupportedOperationException(String.format("Cannot autobox %s of type %s to %s", value, value.getClass().getName(), Integer.class.getName()));
    }
}
