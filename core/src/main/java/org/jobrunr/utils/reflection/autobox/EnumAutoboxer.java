package org.jobrunr.utils.reflection.autobox;

public class EnumAutoboxer implements TypeAutoboxer<Enum> {
    @Override
    public boolean supports(Class<?> type) {
        return type.isEnum();
    }

    @Override
    public Enum autobox(Object value, Class<Enum> type) {
        return Enum.valueOf(type, value.toString());
    }
}
