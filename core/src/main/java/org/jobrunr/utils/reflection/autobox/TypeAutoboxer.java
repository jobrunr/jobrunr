package org.jobrunr.utils.reflection.autobox;

public interface TypeAutoboxer<T> {

    boolean supports(Class<?> type);

    T autobox(Object value, Class<T> type);
}
