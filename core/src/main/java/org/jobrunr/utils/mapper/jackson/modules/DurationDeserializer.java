package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jobrunr.utils.DurationUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;

public class DurationDeserializer extends StdDeserializer<Duration> {

    protected DurationDeserializer() {
        super(Duration.class);
    }

    @Override
    public Duration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        if (jsonParser.getCurrentToken() == JsonToken.VALUE_STRING) {
            return Duration.parse(jsonParser.getText());
        }
        final BigDecimal durationAsSecAndNanoSec = jsonParser.getDecimalValue();
        return DurationUtils.fromBigDecimal(durationAsSecAndNanoSec);
    }
}
