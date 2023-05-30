package org.jobrunr.utils.mapper.jsonb.serializer;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;

public class DurationTypeDeserializer implements JsonbDeserializer<Duration> {

    @Override
    public Duration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Type type) {
        final BigDecimal durationAsSecAndNanoSec = jsonParser.getBigDecimal();
        return Duration.ofSeconds(
                durationAsSecAndNanoSec.longValue(),
                durationAsSecAndNanoSec.remainder(BigDecimal.ONE).movePointRight(durationAsSecAndNanoSec.scale()).abs().longValue()
        );
    }
}
