package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;


public class DurationSerializer extends StdSerializer<Duration> {

    protected DurationSerializer() {
        super(Duration.class);
    }

    @Override
    public void serialize(Duration duration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeNumber(BigDecimal.valueOf(duration.toNanos()).scaleByPowerOfTen(-9).toString()); // nanos = 9
    }

}
