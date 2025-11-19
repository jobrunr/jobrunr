package org.jobrunr.utils.mapper.jackson3.modules;

import org.jobrunr.utils.DurationUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.math.BigDecimal;
import java.time.Duration;

public class DurationDeserializer extends StdDeserializer<Duration> {

    protected DurationDeserializer() {
        super(Duration.class);
    }

    @Override
    public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return Duration.parse(p.getText());
        }
        final BigDecimal durationAsSecAndNanoSec = p.getDecimalValue();
        return DurationUtils.fromBigDecimal(durationAsSecAndNanoSec);
    }

}
