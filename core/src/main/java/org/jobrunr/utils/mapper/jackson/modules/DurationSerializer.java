package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.jobrunr.utils.DurationUtils;

import java.io.IOException;
import java.time.Duration;


public class DurationSerializer extends StdSerializer<Duration> {

    protected DurationSerializer() {
        super(Duration.class);
    }

    @Override
    public void serialize(Duration duration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeNumber(DurationUtils.toBigDecimal(duration).toString());
    }

}
