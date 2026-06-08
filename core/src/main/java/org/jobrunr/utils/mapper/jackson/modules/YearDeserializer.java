package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Year;

public class YearDeserializer extends StdDeserializer<Year> {

    protected YearDeserializer() {
        super(Year.class);
    }

    @Override
    public Year deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return Year.parse(jsonParser.getText());
    }
}
