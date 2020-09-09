package org.jobrunr.utils.reflection.autobox;

import java.util.UUID;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class UUIDTypeAutoboxer implements TypeAutoboxer<UUID> {
    @Override
    public boolean supports(Class<?> type) {
        return UUID.class.equals(type);
    }

    @Override
    public UUID autobox(Object value, Class<UUID> type) {
        if (value instanceof UUID) {
            return (UUID) value;
        } else if (value instanceof String) {
            return cast(UUID.fromString((String) value));
        }
        throw new UnsupportedOperationException(String.format("Cannot autobox %s of type %s to %s", value, value.getClass().getName(), UUID.class.getName()));
    }
}
