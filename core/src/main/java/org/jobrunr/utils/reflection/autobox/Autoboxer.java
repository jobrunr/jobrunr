package org.jobrunr.utils.reflection.autobox;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class Autoboxer {

    private static final List<TypeAutoboxer<?>> autoboxers = Arrays.asList(
            new BooleanTypeAutoboxer(),
            new InstantTypeAutoboxer(),
            new IntegerTypeAutoboxer(),
            new LongTypeAutoboxer(),
            new DoubleTypeAutoboxer(),
            new FloatTypeAutoboxer(),
            new StringTypeAutoboxer(),
            new UUIDTypeAutoboxer(),
            new EnumAutoboxer(),
            new DurationTypeAutoboxer()
    );

    private Autoboxer() {

    }

    public static <T> T autobox(Object value, Class<T> type) {
        if (value == null) return null;
        if (type.isAssignableFrom(value.getClass())) {
            return cast(value);
        }

        return cast(findAutoboxer(type)
                .map(autoboxer -> autoboxer.autobox(value, type))
                .orElseThrow(() -> new UnsupportedOperationException(String.format("Cannot autobox %s of type %s to %s", value, value.getClass().getName(), type.getName()))));
    }

    public static boolean supports(Class<?> type) {
        return findAutoboxer(type).isPresent();
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<TypeAutoboxer<T>> findAutoboxer(Class<T> type) {
        return autoboxers.stream()
                .filter(autoboxer -> autoboxer.supports(type))
                .map(a -> (TypeAutoboxer<T>) a)
                .findFirst();
    }
}
