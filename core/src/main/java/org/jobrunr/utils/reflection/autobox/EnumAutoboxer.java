package org.jobrunr.utils.reflection.autobox;

@SuppressWarnings({"rawtypes", "unchecked"})
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
