package org.jobrunr.utils.reflection.autobox;

import java.math.BigDecimal;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class FloatTypeAutoboxer implements TypeAutoboxer<Float> {
    @Override
    public boolean supports(Class<?> type) {
        return float.class.equals(type) || Float.class.equals(type);
    }

    @Override
    public Float autobox(Object value, Class<Float> type) {
        if (value instanceof Float) {
            return cast(value);
        } else if (value instanceof BigDecimal) {
            return cast(((BigDecimal) value).floatValue());
        } else if (value instanceof String) {
            return cast(Long.valueOf((String) value));
        }
        throw new UnsupportedOperationException(String.format("Cannot autobox %s of type %s to %s", value, value.getClass().getName(), Long.class.getName()));
    }
}
