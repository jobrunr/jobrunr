package org.jobrunr.utils.mapper.jackson3.modules;

import org.jobrunr.utils.DurationUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.time.Duration;

public class DurationSerializer extends StdSerializer<Duration> {

    protected DurationSerializer() {
        super(Duration.class);
    }

    @Override
    public void serialize(Duration value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeNumber(DurationUtils.toBigDecimal(value).toString());
    }
}
