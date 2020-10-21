package org.jobrunr.utils.reflection.autobox;

import java.util.Arrays;
import java.util.List;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class Autoboxer {

    private static final List<TypeAutoboxer> autoboxers = Arrays.asList(
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

    @SuppressWarnings("unchecked")
    public static <T> T autobox(Object value, Class<T> type) {
        if (type.equals(value.getClass())) {
            return cast(value);
        }

        return cast(autoboxers.stream()
                .filter(autoboxer -> autoboxer.supports(type))
                .findFirst()
                .map(autoboxer -> autoboxer.autobox(value, type))
                .orElseThrow(() -> new UnsupportedOperationException(String.format("Cannot autobox %s of type %s to %s", value, value.getClass().getName(), type.getName()))));

    }
}
