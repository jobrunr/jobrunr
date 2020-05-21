package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;

public class DurationDeserializer extends StdDeserializer<Duration> {

    protected DurationDeserializer() {
        super(Duration.class);
    }

    @Override
    public Duration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        final BigDecimal durationAsSecAndNanoSec = jsonParser.getDecimalValue();
        return Duration.ofSeconds(
                durationAsSecAndNanoSec.longValue(),
                durationAsSecAndNanoSec.remainder(BigDecimal.ONE).movePointRight(durationAsSecAndNanoSec.scale()).abs().longValue()
        );
    }
}
