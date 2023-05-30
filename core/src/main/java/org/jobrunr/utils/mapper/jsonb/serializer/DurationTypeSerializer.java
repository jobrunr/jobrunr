package org.jobrunr.utils.mapper.jsonb.serializer;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Serializer for {@link Duration} type.
 */
public class DurationTypeSerializer implements JsonbSerializer<Duration> {

    @Override
    public void serialize(Duration duration, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
        jsonGenerator.write(new BigDecimal(duration.getSeconds() + "." + String.format("%09d", duration.getNano())));
    }
}
