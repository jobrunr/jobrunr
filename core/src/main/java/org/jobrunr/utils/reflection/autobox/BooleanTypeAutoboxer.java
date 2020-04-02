package org.jobrunr.utils.reflection.autobox;

import java.math.BigDecimal;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class BooleanTypeAutoboxer implements TypeAutoboxer<Boolean> {
    @Override
    public boolean supports(Class<?> type) {
        return Boolean.class.equals(type) || boolean.class.equals(type);
    }

    @Override
    public Boolean autobox(Object value) {
        if (value instanceof BigDecimal) {
            return cast(!BigDecimal.ZERO.equals(value));
        } else if (value instanceof Integer) {
            return cast(((Integer) value) != 0);
        }
        throw new UnsupportedOperationException(String.format("Cannot autobox %s of type %s to %s", value, value.getClass().getName(), Boolean.class.getName()));
    }
}
