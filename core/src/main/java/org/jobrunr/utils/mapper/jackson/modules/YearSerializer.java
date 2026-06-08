package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Year;

public class YearSerializer extends StdSerializer<Year> {

    protected YearSerializer() {
        super(Year.class);
    }

    @Override
    public void serialize(Year year, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(year.toString());
    }
}
