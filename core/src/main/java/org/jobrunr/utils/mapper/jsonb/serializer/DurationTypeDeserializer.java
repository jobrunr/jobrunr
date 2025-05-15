package org.jobrunr.utils.mapper.jsonb.serializer;

import jakarta.json.JsonValue;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import org.jobrunr.utils.DurationUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;

public class DurationTypeDeserializer implements JsonbDeserializer<Duration> {

    @Override
    public Duration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Type type) {
        JsonValue value = jsonParser.getValue();
        if (value != JsonValue.NULL) {
            final BigDecimal durationAsSecAndNanoSec = jsonParser.getBigDecimal();
            return DurationUtils.fromBigDecimal(durationAsSecAndNanoSec);
        }
        return null;
    }
}
